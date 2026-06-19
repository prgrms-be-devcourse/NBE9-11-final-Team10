package com.team10.backend.domain.investment.realtime.repository;

import static com.team10.backend.domain.investment.realtime.RealtimeOrderbookRedisConstants.ALL_KEYS_PATTERN;
import static com.team10.backend.domain.investment.realtime.RealtimeOrderbookRedisConstants.STOCK_STREAMS_KEY_PREFIX;
import static com.team10.backend.domain.investment.realtime.RealtimeOrderbookRedisConstants.STREAMS_KEY_SUFFIX;
import static com.team10.backend.domain.investment.realtime.RealtimeOrderbookRedisConstants.USER_STREAMS_KEY_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("test")
class RealtimeOrderbookSubscriptionStoreIntegrationTest {

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private RealtimeOrderbookSubscriptionStore subscriptionStore;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        Set<String> keys = redisTemplate.keys(ALL_KEYS_PATTERN);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Test
    @DisplayName("실시간 호가 구독 상태를 저장하고 streamId로 조회한다")
    void saveAndFindByStreamId() {
        RealtimeOrderbookSubscription subscription = subscription(
                "stream-1",
                1L,
                "005930",
                "instance-a"
        );

        subscriptionStore.save(subscription);

        assertThat(subscriptionStore.findByStreamId("stream-1"))
                .contains(subscription);
        assertThat(subscriptionStore.findActiveStreamIdsByStockCode("005930"))
                .containsExactly("stream-1");
        assertThat(subscriptionStore.findActiveStreamIdsByUserId(1L))
                .containsExactly("stream-1");
    }

    @Test
    @DisplayName("같은 종목을 구독하는 여러 스트림을 종목 기준으로 집계한다")
    void findActiveStreamsByStockCode() {
        subscriptionStore.save(subscription("stream-1", 1L, "005930", "instance-a"));
        subscriptionStore.save(subscription("stream-2", 2L, "005930", "instance-b"));
        subscriptionStore.save(subscription("stream-3", 1L, "000660", "instance-a"));

        assertThat(subscriptionStore.findActiveStreamIdsByStockCode("005930"))
                .containsExactlyInAnyOrder("stream-1", "stream-2");
        assertThat(subscriptionStore.countActiveStreamsByStockCode("005930"))
                .isEqualTo(2);
        assertThat(subscriptionStore.findActiveStockCodes())
                .containsExactlyInAnyOrder("005930", "000660");
    }

    @Test
    @DisplayName("streamId 삭제 시 사용자/종목 인덱스에서도 제거한다")
    void deleteByStreamId() {
        subscriptionStore.save(subscription("stream-1", 1L, "005930", "instance-a"));

        assertThat(subscriptionStore.deleteByStreamId("stream-1"))
                .contains(subscription("stream-1", 1L, "005930", "instance-a"));

        assertThat(subscriptionStore.findByStreamId("stream-1")).isEmpty();
        assertThat(subscriptionStore.findActiveStreamIdsByStockCode("005930")).isEmpty();
        assertThat(subscriptionStore.findActiveStreamIdsByUserId(1L)).isEmpty();
    }

    @Test
    @DisplayName("streamId와 사용자 ID가 일치할 때만 구독 상태를 삭제한다")
    void deleteByStreamIdAndUserId() {
        subscriptionStore.save(subscription("stream-1", 1L, "005930", "instance-a"));

        assertThat(subscriptionStore.deleteByStreamIdAndUserId("stream-1", 2L))
                .isEmpty();
        assertThat(subscriptionStore.findByStreamId("stream-1"))
                .contains(subscription("stream-1", 1L, "005930", "instance-a"));

        assertThat(subscriptionStore.deleteByStreamIdAndUserId("stream-1", 1L))
                .contains(subscription("stream-1", 1L, "005930", "instance-a"));
        assertThat(subscriptionStore.findByStreamId("stream-1")).isEmpty();
    }

    @Test
    @DisplayName("lease가 사라진 stale stream은 활성 구독 조회에서 제외하고 인덱스에서 제거한다")
    void staleStreamIsFilteredAndRemoved() {
        subscriptionStore.save(subscription("stream-active", 1L, "005930", "instance-a"));
        redisTemplate.opsForSet().add(stockStreamsKey("005930"), "stream-stale");
        redisTemplate.opsForSet().add(userStreamsKey(1L), "stream-stale");

        assertThat(subscriptionStore.findActiveStreamIdsByStockCode("005930"))
                .containsExactly("stream-active");
        assertThat(subscriptionStore.findActiveStreamIdsByUserId(1L))
                .containsExactly("stream-active");
        assertThat(redisTemplate.opsForSet().members(stockStreamsKey("005930")))
                .doesNotContain("stream-stale");
        assertThat(redisTemplate.opsForSet().members(userStreamsKey(1L)))
                .doesNotContain("stream-stale");
    }

    @Test
    @DisplayName("존재하는 streamId의 lease를 갱신하고 삭제된 streamId는 갱신하지 않는다")
    void renewLease() {
        subscriptionStore.save(subscription("stream-1", 1L, "005930", "instance-a"));

        assertThat(subscriptionStore.renewLease("stream-1")).isTrue();

        subscriptionStore.deleteByStreamId("stream-1");

        assertThat(subscriptionStore.renewLease("stream-1")).isFalse();
    }

    private RealtimeOrderbookSubscription subscription(
            String streamId,
            Long userId,
            String stockCode,
            String ownerInstanceId
    ) {
        return new RealtimeOrderbookSubscription(streamId, userId, stockCode, ownerInstanceId);
    }

    private String stockStreamsKey(String stockCode) {
        return STOCK_STREAMS_KEY_PREFIX + stockCode + STREAMS_KEY_SUFFIX;
    }

    private String userStreamsKey(Long userId) {
        return USER_STREAMS_KEY_PREFIX + userId + STREAMS_KEY_SUFFIX;
    }
}
