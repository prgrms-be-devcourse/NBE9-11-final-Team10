package com.team10.backend.domain.transfer.scheduler;

import com.team10.backend.domain.transfer.service.TransferIdempotencyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransferIdempotencyCleanupSchedulerTest {

    @Mock
    private TransferIdempotencyService transferIdempotencyService;

    @InjectMocks
    private TransferIdempotencyCleanupScheduler scheduler;

    @Test
    @DisplayName("스케줄러는 10분 이상 방치된 PROCESSING 멱등성 레코드 만료 처리를 호출한다")
    void expireStaleProcessing_callsServiceWithTenMinuteTimeout() {
        scheduler.expireStaleProcessing();

        verify(transferIdempotencyService).expireStaleProcessing(Duration.ofMinutes(10));
    }
}
