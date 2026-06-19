package com.team10.backend.domain.investment.realtime.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team10.backend.domain.investment.realtime.service.RealtimeOrderbookSseEmitterRegistry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimeOrderbookSubscriptionChangedEventListener implements MessageListener {

    private final ObjectMapper objectMapper;
    private final RealtimeOrderbookSseEmitterRegistry emitterRegistry;

    /**
     * Redis Pub/Sub 메시지 수신 진입점. json 역직렬화 및 이벤트 종류에 맞는 로직 수행
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            RealtimeOrderbookSubscriptionChangedEvent event = objectMapper.readValue(
                    message.getBody(),
                    RealtimeOrderbookSubscriptionChangedEvent.class
            );
            handle(event);
        } catch (IOException e) {
            log.warn("Failed to parse realtime orderbook subscription changed event. payload={}",
                    new String(message.getBody(), StandardCharsets.UTF_8),
                    e);
        }
    }

    private void handle(RealtimeOrderbookSubscriptionChangedEvent event) {
        if (event.eventType() != RealtimeOrderbookSubscriptionEventType.ENDED) {
            return;
        }

        emitterRegistry.complete(event.streamId());
    }
}
