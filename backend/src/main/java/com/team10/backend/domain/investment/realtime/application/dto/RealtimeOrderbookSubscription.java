package com.team10.backend.domain.investment.realtime.application.dto;

public record RealtimeOrderbookSubscription(
        String streamId,
        Long userId,
        String stockCode,
        String ownerInstanceId
) {
}
