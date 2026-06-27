package com.team10.backend.global.idempotency.scheduler;

import com.team10.backend.global.idempotency.service.IdempotencyService;
import com.team10.backend.global.lock.DistributedLockTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyCleanupScheduler {

    private static final String LOCK_KEY = "lock:scheduler:idempotency-cleanup";
    private static final Duration LOCK_LEASE_TIME = Duration.ofMinutes(5);

    private final IdempotencyService idempotencyService;
    private final DistributedLockTemplate lockTemplate;

    @Scheduled(fixedDelayString = "${idempotency.cleanup-fixed-delay-ms:60000}")
    public void expireStaleProcessing() {
        try {
            // 오래된 멱등성 레코드 정리는 모든 인스턴스가 동시에 수행할 필요가 없으므로,
            // 하나의 인스턴스만 정리하고 나머지는 정상적으로 건너뛴다.
            boolean executed = lockTemplate.tryExecuteWithLock(
                    LOCK_KEY,
                    LOCK_LEASE_TIME,
                    () -> {
                        int expiredCount = idempotencyService.expireStaleProcessing(Duration.ofMinutes(10));
                        int deletedCount = idempotencyService.deleteRecordsOlderThan(Duration.ofDays(15));
                        log.info("멱등성 레코드 정리 완료. expiredCount={}, deletedCount={}", expiredCount, deletedCount);
                    }
            );

            if (!executed) {
                log.info("다른 인스턴스에서 멱등성 레코드 정리가 실행 중이므로 이번 실행을 건너뜁니다.");
            }
        } catch (RuntimeException e) {
            log.warn("멱등성 레코드 정리 실패", e);
        }
    }
}
