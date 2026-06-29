package com.team10.backend.domain.investment.realtime.domain.event;

import com.team10.backend.domain.investment.realtime.application.dto.RealtimeOrderbookSubscription;

public record RealtimeOrderbookSubscriptionChangedEvent(
        String streamId,
        Long userId,
        String stockCode,
        EventType eventType
) {

    public static RealtimeOrderbookSubscriptionChangedEvent started(
            RealtimeOrderbookSubscription subscription
    ) {
        return new RealtimeOrderbookSubscriptionChangedEvent(
                subscription.streamId(),
                subscription.userId(),
                subscription.stockCode(),
                EventType.STARTED
        );
    }

    public static RealtimeOrderbookSubscriptionChangedEvent ended(
            RealtimeOrderbookSubscription subscription
    ) {
        return new RealtimeOrderbookSubscriptionChangedEvent(
                subscription.streamId(),
                subscription.userId(),
                subscription.stockCode(),
                EventType.ENDED
        );
    }

    public enum EventType {
        STARTED,
        ENDED
    }
}
