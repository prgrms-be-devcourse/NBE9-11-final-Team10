package com.team10.backend.domain.investment.realtime.application.dto;

public record RealtimeOrderbookStreamCreatedRes(
        String streamId,
        String stockCode
) {
}
