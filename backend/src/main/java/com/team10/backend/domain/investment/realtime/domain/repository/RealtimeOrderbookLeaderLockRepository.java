package com.team10.backend.domain.investment.realtime.domain.repository;

import static com.team10.backend.domain.investment.realtime.infrastructure.config.RealtimeOrderbookRedisConstants.LEADER_KEY;
import static com.team10.backend.domain.investment.realtime.infrastructure.config.RealtimeOrderbookRedisConstants.LEADER_LEASE_TTL;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
@RequiredArgsConstructor
public class RealtimeOrderbookLeaderLockRepository {

    /**
     * leader instance인 경우 leader lock 연장
     */
    private static final RedisScript<Long> RENEW_IF_OWNER_SCRIPT = RedisScript.of(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then\n" +
                    "  redis.call('EXPIRE', KEYS[1], ARGV[2])\n" +
                    "  return 1\n" +
                    "end\n" +
                    "return 0",
            Long.class
    );

    /**
     * leader instance인 경우 leader lock 반납
     */
    private static final RedisScript<Long> RELEASE_IF_OWNER_SCRIPT = RedisScript.of(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then\n" +
                    "  redis.call('DEL', KEYS[1])\n" +
                    "  return 1\n" +
                    "end\n" +
                    "return 0",
            Long.class
    );

    private final StringRedisTemplate redisTemplate;

    /**
     * leader lo도ck 획득 시도
     */
    public boolean tryAcquire(String instanceId) {
        validateInstanceId(instanceId);

        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(LEADER_KEY, instanceId, LEADER_LEASE_TTL);

        return Boolean.TRUE.equals(acquired);
    }

    /**
     * 실행 인스턴스가 leader인 경우에만 lock 연장
     */
    public boolean renew(String instanceId) {
        validateInstanceId(instanceId);

        Long renewed = redisTemplate.execute(
                RENEW_IF_OWNER_SCRIPT,
                List.of(LEADER_KEY),
                instanceId,
                String.valueOf(LEADER_LEASE_TTL.toSeconds())
        );

        return Long.valueOf(1L).equals(renewed);
    }

    /**
     * 실행 인스턴스가 leader인 경우에만 lock 반환
     */
    public boolean release(String instanceId) {
        validateInstanceId(instanceId);

        Long released = redisTemplate.execute(
                RELEASE_IF_OWNER_SCRIPT,
                List.of(LEADER_KEY),
                instanceId
        );

        return Long.valueOf(1L).equals(released);
    }

    public boolean isOwnedBy(String instanceId) {
        validateInstanceId(instanceId);
        return instanceId.equals(redisTemplate.opsForValue().get(LEADER_KEY));
    }

    public long leaseTtlSeconds() {
        return LEADER_LEASE_TTL.toSeconds();
    }

    private void validateInstanceId(String instanceId) {
        if (!StringUtils.hasText(instanceId)) {
            throw new IllegalArgumentException("instanceId must not be blank");
        }
    }
}
