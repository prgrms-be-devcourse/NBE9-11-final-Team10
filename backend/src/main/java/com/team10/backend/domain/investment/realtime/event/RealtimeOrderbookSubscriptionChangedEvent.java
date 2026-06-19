package com.team10.backend.domain.investment.realtime.event;

import com.team10.backend.domain.investment.realtime.repository.RealtimeOrderbookSubscription;

public record RealtimeOrderbookSubscriptionChangedEvent(
        String streamId,
        Long userId,
        String stockCode,
        RealtimeOrderbookSubscriptionEventType eventType
) {

    public static RealtimeOrderbookSubscriptionChangedEvent started(
            RealtimeOrderbookSubscription subscription
    ) {
        return new RealtimeOrderbookSubscriptionChangedEvent(
                subscription.streamId(),
                subscription.userId(),
                subscription.stockCode(),
                RealtimeOrderbookSubscriptionEventType.STARTED
        );
    }

    public static RealtimeOrderbookSubscriptionChangedEvent ended(
            RealtimeOrderbookSubscription subscription
    ) {
        return new RealtimeOrderbookSubscriptionChangedEvent(
                subscription.streamId(),
                subscription.userId(),
                subscription.stockCode(),
                RealtimeOrderbookSubscriptionEventType.ENDED
        );
    }
}
