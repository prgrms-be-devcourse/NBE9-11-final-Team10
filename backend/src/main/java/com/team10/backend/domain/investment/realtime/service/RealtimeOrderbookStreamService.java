package com.team10.backend.domain.investment.realtime.service;

import com.team10.backend.domain.investment.config.KisProperties;
import com.team10.backend.domain.investment.exception.InvestmentErrorCode;
import com.team10.backend.domain.investment.realtime.config.RealtimeOrderbookSseConstants;
import com.team10.backend.domain.investment.realtime.dto.RealtimeOrderbookStreamCreatedRes;
import com.team10.backend.domain.investment.realtime.event.subcriptionchange.RealtimeOrderbookSubscriptionChangedEvent;
import com.team10.backend.domain.investment.realtime.event.subcriptionchange.RealtimeOrderbookSubscriptionChangedEventPublisher;
import com.team10.backend.domain.investment.realtime.repository.RealtimeOrderbookSubscription;
import com.team10.backend.domain.investment.realtime.repository.RealtimeOrderbookSubscriptionStore;
import com.team10.backend.domain.investment.stock.entity.Stock;
import com.team10.backend.domain.investment.stock.repository.StockRepository;
import com.team10.backend.global.exception.BusinessException;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
public class RealtimeOrderbookStreamService {

    private final StockRepository stockRepository;
    private final RealtimeOrderbookSseEmitterRegistry emitterRegistry;
    private final RealtimeOrderbookSubscriptionStore subscriptionStore;
    private final RealtimeOrderbookInstanceIdProvider instanceIdProvider;
    private final RealtimeOrderbookSubscriptionChangedEventPublisher eventPublisher;
    private final KisProperties kisProperties;

    public RealtimeOrderbookSseConnection openStream(Long userId, String stockCode) {
        validateTradableStock(stockCode);

        RealtimeOrderbookSseConnection connection = emitterRegistry.register(
                userId,
                stockCode,
                this::closeStreamByEmitterTermination
        );

        RealtimeOrderbookSubscription subscription = new RealtimeOrderbookSubscription(
                connection.streamId(),
                userId,
                stockCode,
                instanceIdProvider.getInstanceId()
        );

        try {
            if (!subscriptionStore.saveIfWithinActiveStockLimit(
                    subscription,
                    kisProperties.websocketMaxSubscriptions()
            )) {
                throw new BusinessException(InvestmentErrorCode.REALTIME_ORDERBOOK_SUBSCRIPTION_LIMIT_EXCEEDED);
            }
            eventPublisher.publish(RealtimeOrderbookSubscriptionChangedEvent.started(subscription));
            sendStreamCreatedEvent(connection);
            return connection;
        } catch (RuntimeException e) {
            emitterRegistry.complete(connection.streamId());
            throw e;
        }
    }

    public void closeStream(Long userId, String streamId) {
        RealtimeOrderbookSubscription subscription = subscriptionStore
                .deleteByStreamIdAndUserId(streamId, userId)
                .orElseThrow(() -> new BusinessException(InvestmentErrorCode.REALTIME_ORDERBOOK_STREAM_NOT_FOUND));

        eventPublisher.publish(RealtimeOrderbookSubscriptionChangedEvent.ended(subscription));
        emitterRegistry.complete(streamId);
    }

    private void closeStreamByEmitterTermination(String streamId) {
        if (streamId == null) {
            return;
        }

        subscriptionStore.deleteByStreamId(streamId)
                .map(RealtimeOrderbookSubscriptionChangedEvent::ended)
                .ifPresent(eventPublisher::publish);
    }

    private void sendStreamCreatedEvent(RealtimeOrderbookSseConnection connection) {
        try {
            connection.emitter().send(SseEmitter.event()
                    .name(RealtimeOrderbookSseConstants.STREAM_CREATED_EVENT_NAME)
                    .data(new RealtimeOrderbookStreamCreatedRes(
                            connection.streamId(),
                            connection.stockCode()
                    )));
        } catch (IOException | IllegalStateException e) {
            throw new IllegalStateException("실시간 호가 SSE 스트림 생성 이벤트 전송에 실패했습니다.", e);
        }
    }

    private void validateTradableStock(String stockCode) {
        Stock stock = stockRepository.findByStockCode(stockCode)
                .orElseThrow(() -> new BusinessException(InvestmentErrorCode.STOCK_NOT_FOUND));

        if (!stock.isTradable()) {
            throw new BusinessException(InvestmentErrorCode.STOCK_NOT_TRADABLE);
        }
    }
}
