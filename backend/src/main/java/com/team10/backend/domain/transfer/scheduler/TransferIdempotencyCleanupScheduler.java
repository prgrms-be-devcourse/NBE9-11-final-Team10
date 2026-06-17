package com.team10.backend.domain.transfer.scheduler;

import com.team10.backend.domain.transfer.service.TransferIdempotencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class TransferIdempotencyCleanupScheduler {

    private final TransferIdempotencyService transferIdempotencyService;

    @Scheduled(fixedDelayString = "${transfer.idempotency.cleanup-fixed-delay-ms:60000}")
    public void expireStaleProcessing() {
        transferIdempotencyService.expireStaleProcessing(Duration.ofMinutes(10));
    }
}
