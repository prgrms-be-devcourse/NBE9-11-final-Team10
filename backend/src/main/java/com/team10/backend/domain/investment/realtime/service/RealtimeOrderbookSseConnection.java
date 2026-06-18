package com.team10.backend.domain.investment.realtime.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public record RealtimeOrderbookSseConnection(
        String streamId,
        Long userId,
        String stockCode,
        SseEmitter emitter
) {
}
