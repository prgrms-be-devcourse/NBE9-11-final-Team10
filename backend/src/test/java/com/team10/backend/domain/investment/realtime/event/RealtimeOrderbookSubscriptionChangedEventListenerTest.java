package com.team10.backend.domain.investment.realtime.event;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team10.backend.domain.investment.realtime.config.RealtimeOrderbookRedisConstants;
import com.team10.backend.domain.investment.realtime.event.subcriptionchange.RealtimeOrderbookSubscriptionChangedEvent;
import com.team10.backend.domain.investment.realtime.event.subcriptionchange.RealtimeOrderbookSubscriptionChangedEventListener;
import com.team10.backend.domain.investment.realtime.event.subcriptionchange.RealtimeOrderbookSubscriptionEventType;
import com.team10.backend.domain.investment.realtime.service.RealtimeOrderbookKisSubscriptionCoordinator;
import com.team10.backend.domain.investment.realtime.service.RealtimeOrderbookSseEmitterRegistry;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.DefaultMessage;

class RealtimeOrderbookSubscriptionChangedEventListenerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RealtimeOrderbookSseEmitterRegistry emitterRegistry =
            org.mockito.Mockito.mock(RealtimeOrderbookSseEmitterRegistry.class);
    private final RealtimeOrderbookKisSubscriptionCoordinator kisSubscriptionCoordinator =
            org.mockito.Mockito.mock(RealtimeOrderbookKisSubscriptionCoordinator.class);
    private final RealtimeOrderbookSubscriptionChangedEventListener listener =
            new RealtimeOrderbookSubscriptionChangedEventListener(
                    objectMapper,
                    emitterRegistry,
                    kisSubscriptionCoordinator
            );

    @Test
    @DisplayName("ENDED 이벤트를 받으면 로컬 Registry의 동일 streamId를 종료한다")
    void handleEndedEvent() throws Exception {
        RealtimeOrderbookSubscriptionChangedEvent event = new RealtimeOrderbookSubscriptionChangedEvent(
                "stream-1",
                1L,
                "005930",
                RealtimeOrderbookSubscriptionEventType.ENDED
        );

        listener.onMessage(message(event), null);

        verify(emitterRegistry).complete("stream-1");
        verify(kisSubscriptionCoordinator).reconcileStock("005930");
    }

    @Test
    @DisplayName("STARTED 이벤트는 로컬 SSE 연결을 종료하지 않고 KIS 구독 상태를 조정한다")
    void ignoreStartedEvent() throws Exception {
        RealtimeOrderbookSubscriptionChangedEvent event = new RealtimeOrderbookSubscriptionChangedEvent(
                "stream-1",
                1L,
                "005930",
                RealtimeOrderbookSubscriptionEventType.STARTED
        );

        listener.onMessage(message(event), null);

        verify(emitterRegistry, never()).complete("stream-1");
        verify(kisSubscriptionCoordinator).reconcileStock("005930");
    }

    private DefaultMessage message(RealtimeOrderbookSubscriptionChangedEvent event) throws Exception {
        return new DefaultMessage(
                RealtimeOrderbookRedisConstants.SUBSCRIPTION_CHANGED_CHANNEL.getBytes(StandardCharsets.UTF_8),
                objectMapper.writeValueAsBytes(event)
        );
    }
}
