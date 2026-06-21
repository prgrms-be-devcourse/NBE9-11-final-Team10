package com.team10.backend.domain.investment.realtime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.team10.backend.domain.investment.exception.InvestmentErrorCode;
import com.team10.backend.domain.investment.realtime.event.subcriptionchange.RealtimeOrderbookSubscriptionChangedEvent;
import com.team10.backend.domain.investment.realtime.event.subcriptionchange.RealtimeOrderbookSubscriptionChangedEventPublisher;
import com.team10.backend.domain.investment.realtime.repository.RealtimeOrderbookSubscription;
import com.team10.backend.domain.investment.realtime.repository.RealtimeOrderbookSubscriptionStore;
import com.team10.backend.domain.investment.stock.entity.Stock;
import com.team10.backend.domain.investment.stock.repository.StockRepository;
import com.team10.backend.domain.investment.stock.type.StockMarket;
import com.team10.backend.domain.investment.stock.type.StockStatus;
import com.team10.backend.domain.investment.type.CurrencyCode;
import com.team10.backend.global.exception.BusinessException;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RealtimeOrderbookStreamServiceTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private RealtimeOrderbookSubscriptionStore subscriptionStore;

    @Mock
    private RealtimeOrderbookInstanceIdProvider instanceIdProvider;

    @Mock
    private RealtimeOrderbookSubscriptionChangedEventPublisher eventPublisher;

    private RealtimeOrderbookSseEmitterRegistry emitterRegistry;
    private RealtimeOrderbookStreamService streamService;

    @BeforeEach
    void setUp() {
        emitterRegistry = new RealtimeOrderbookSseEmitterRegistry();
        streamService = new RealtimeOrderbookStreamService(
                stockRepository,
                emitterRegistry,
                subscriptionStore,
                instanceIdProvider,
                eventPublisher
        );
    }

    @Test
    @DisplayName("거래 가능한 종목으로 SSE 스트림을 생성하고 Redis 구독 상태와 STARTED 이벤트를 저장한다")
    void openStream() {
        when(stockRepository.findByStockCode("005930")).thenReturn(Optional.of(stock(StockStatus.ACTIVE)));
        when(instanceIdProvider.getInstanceId()).thenReturn("instance-a");

        RealtimeOrderbookSseConnection connection = streamService.openStream(1L, "005930");

        assertThat(connection.streamId()).isNotBlank();
        assertThat(connection.userId()).isEqualTo(1L);
        assertThat(connection.stockCode()).isEqualTo("005930");
        assertThat(emitterRegistry.find(connection.streamId())).contains(connection);
        assertThat(emitterRegistry.findStreamIdsByStockCode("005930"))
                .containsExactly(connection.streamId());

        ArgumentCaptor<RealtimeOrderbookSubscription> subscriptionCaptor =
                ArgumentCaptor.forClass(RealtimeOrderbookSubscription.class);
        verify(subscriptionStore).save(subscriptionCaptor.capture());
        assertThat(subscriptionCaptor.getValue())
                .isEqualTo(new RealtimeOrderbookSubscription(
                        connection.streamId(),
                        1L,
                        "005930",
                        "instance-a"
                ));

        ArgumentCaptor<RealtimeOrderbookSubscriptionChangedEvent> eventCaptor =
                ArgumentCaptor.forClass(RealtimeOrderbookSubscriptionChangedEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().streamId()).isEqualTo(connection.streamId());
        assertThat(eventCaptor.getValue().userId()).isEqualTo(1L);
        assertThat(eventCaptor.getValue().stockCode()).isEqualTo("005930");
        assertThat(eventCaptor.getValue().eventType()).isEqualTo(RealtimeOrderbookSubscriptionChangedEvent.EventType.STARTED);
    }

    @Test
    @DisplayName("존재하지 않는 종목은 SSE 스트림을 생성할 수 없다")
    void openStreamWithMissingStock() {
        when(stockRepository.findByStockCode("005930")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> streamService.openStream(1L, "005930"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(InvestmentErrorCode.STOCK_NOT_FOUND);

        verify(subscriptionStore, never()).save(any());
        verify(eventPublisher, never()).publish(any());
        assertThat(emitterRegistry.streamCount()).isZero();
    }

    @Test
    @DisplayName("거래 가능 상태가 아닌 종목은 SSE 스트림을 생성할 수 없다")
    void openStreamWithNotTradableStock() {
        when(stockRepository.findByStockCode("005930")).thenReturn(Optional.of(stock(StockStatus.SUSPENDED)));

        assertThatThrownBy(() -> streamService.openStream(1L, "005930"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(InvestmentErrorCode.STOCK_NOT_TRADABLE);

        verify(subscriptionStore, never()).save(any());
        verify(eventPublisher, never()).publish(any());
        assertThat(emitterRegistry.streamCount()).isZero();
    }

    @Test
    @DisplayName("인증 사용자의 streamId이면 구독 상태를 삭제하고 ENDED 이벤트를 발행한다")
    void closeStream() {
        RealtimeOrderbookSubscription subscription =
                new RealtimeOrderbookSubscription("stream-1", 1L, "005930", "instance-a");
        when(subscriptionStore.deleteByStreamIdAndUserId("stream-1", 1L))
                .thenReturn(Optional.of(subscription));

        streamService.closeStream(1L, "stream-1");

        ArgumentCaptor<RealtimeOrderbookSubscriptionChangedEvent> eventCaptor =
                ArgumentCaptor.forClass(RealtimeOrderbookSubscriptionChangedEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().eventType()).isEqualTo(RealtimeOrderbookSubscriptionChangedEvent.EventType.ENDED);
        assertThat(eventCaptor.getValue().streamId()).isEqualTo("stream-1");
    }

    @Test
    @DisplayName("인증 사용자의 streamId가 아니면 스트림 종료에 실패한다")
    void closeStreamWithNotOwnedStream() {
        when(subscriptionStore.deleteByStreamIdAndUserId("stream-1", 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> streamService.closeStream(1L, "stream-1"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(InvestmentErrorCode.REALTIME_ORDERBOOK_STREAM_NOT_FOUND);

        verify(eventPublisher, never()).publish(any());
    }

    private Stock stock(StockStatus status) {
        return Stock.create(
                "005930",
                "KR7005930003",
                "삼성전자",
                StockMarket.KOSPI,
                CurrencyCode.KRW,
                status,
                LocalDate.of(1975, 6, 11),
                1_000_000L,
                2_000_000L,
                3_000_000L,
                4_000_000L,
                5_000_000L
        );
    }
}
