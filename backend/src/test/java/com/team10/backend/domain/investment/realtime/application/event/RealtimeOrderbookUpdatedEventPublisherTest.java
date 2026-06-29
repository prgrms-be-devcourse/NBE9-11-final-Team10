package com.team10.backend.domain.investment.realtime.application.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team10.backend.domain.investment.realtime.infrastructure.config.RealtimeOrderbookRedisConstants;
import com.team10.backend.domain.investment.realtime.application.dto.RealtimeOrderbookLevel;
import com.team10.backend.domain.investment.realtime.application.dto.RealtimeOrderbookSnapshot;
import com.team10.backend.domain.investment.realtime.application.event.RealtimeOrderbookUpdatedEventPublisher;
import com.team10.backend.domain.investment.realtime.domain.repository.RealtimeOrderbookSnapshotStore;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;

class RealtimeOrderbookUpdatedEventPublisherTest {

    private final StringRedisTemplate redisTemplate = Mockito.mock(StringRedisTemplate.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RealtimeOrderbookSnapshotStore snapshotStore = Mockito.mock(RealtimeOrderbookSnapshotStore.class);
    private final RealtimeOrderbookUpdatedEventPublisher publisher =
            new RealtimeOrderbookUpdatedEventPublisher(redisTemplate, objectMapper, snapshotStore);

    @Test
    @DisplayName("호가 갱신 이벤트를 Redis orderbook-updated 채널로 발행한다")
    void publish() {
        RealtimeOrderbookSnapshot snapshot = new RealtimeOrderbookSnapshot(
                "005930",
                "145856",
                "0",
                List.of(new RealtimeOrderbookLevel(1, 358500L, 53949L)),
                List.of(new RealtimeOrderbookLevel(1, 358000L, 40154L)),
                796206L,
                227494L
        );

        publisher.publish(snapshot);

        verify(snapshotStore).save(snapshot);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).convertAndSend(
                Mockito.eq(RealtimeOrderbookRedisConstants.ORDERBOOK_UPDATED_CHANNEL),
                payloadCaptor.capture()
        );
        assertThat(payloadCaptor.getValue()).contains("\"stockCode\":\"005930\"");
    }
}
