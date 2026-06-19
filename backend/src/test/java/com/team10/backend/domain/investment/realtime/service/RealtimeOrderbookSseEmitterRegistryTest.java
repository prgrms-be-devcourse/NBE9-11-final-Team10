package com.team10.backend.domain.investment.realtime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RealtimeOrderbookSseEmitterRegistryTest {

    private RealtimeOrderbookSseEmitterRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new RealtimeOrderbookSseEmitterRegistry();
    }

    @Test
    @DisplayName("스트림을 등록하면 streamId 기준 연결과 stockCode 기준 라우팅 인덱스를 저장한다")
    void register() {
        AtomicInteger terminatedCount = new AtomicInteger();

        RealtimeOrderbookSseConnection connection =
                registry.register(1L, "005930", streamId -> terminatedCount.incrementAndGet());

        assertThat(connection.streamId()).isNotBlank();
        assertThat(connection.userId()).isEqualTo(1L);
        assertThat(connection.stockCode()).isEqualTo("005930");
        assertThat(connection.emitter()).isNotNull();

        assertThat(registry.find(connection.streamId())).contains(connection);
        assertThat(registry.findStreamIdsByStockCode("005930")).containsExactly(connection.streamId());
        assertThat(registry.streamCount()).isEqualTo(1);
        assertThat(registry.streamCountByStockCode("005930")).isEqualTo(1);
        assertThat(terminatedCount).hasValue(0);
    }

    @Test
    @DisplayName("같은 사용자의 여러 탭 스트림을 서로 다른 종목으로 등록할 수 있다")
    void registerMultipleStreamsForSameUser() {
        RealtimeOrderbookSseConnection first = registry.register(1L, "005930", null);
        RealtimeOrderbookSseConnection second = registry.register(1L, "000660", null);

        assertThat(first.streamId()).isNotEqualTo(second.streamId());
        assertThat(registry.streamCount()).isEqualTo(2);
        assertThat(registry.findStreamIdsByStockCode("005930")).containsExactly(first.streamId());
        assertThat(registry.findStreamIdsByStockCode("000660")).containsExactly(second.streamId());
    }

    @Test
    @DisplayName("같은 종목을 구독하는 여러 스트림을 stockCode 라우팅 인덱스에 함께 저장한다")
    void registerMultipleStreamsForSameStockCode() {
        RealtimeOrderbookSseConnection first = registry.register(1L, "005930", null);
        RealtimeOrderbookSseConnection second = registry.register(2L, "005930", null);

        assertThat(registry.streamCount()).isEqualTo(2);
        assertThat(registry.findStreamIdsByStockCode("005930"))
                .containsExactlyInAnyOrder(first.streamId(), second.streamId());
        assertThat(registry.streamCountByStockCode("005930")).isEqualTo(2);
    }

    @Test
    @DisplayName("스트림 종료 시 연결과 stockCode 라우팅 인덱스를 제거하고 종료 콜백은 한 번만 실행한다")
    void complete() {
        AtomicInteger terminatedCount = new AtomicInteger();
        AtomicReference<String> terminatedStreamId = new AtomicReference<>();
        RealtimeOrderbookSseConnection connection =
                registry.register(1L, "005930", streamId -> {
                    terminatedCount.incrementAndGet();
                    terminatedStreamId.set(streamId);
                });

        assertThat(registry.complete(connection.streamId())).isTrue();
        assertThat(registry.complete(connection.streamId())).isFalse();

        assertThat(registry.find(connection.streamId())).isEmpty();
        assertThat(registry.findStreamIdsByStockCode("005930")).isEmpty();
        assertThat(registry.streamCount()).isZero();
        assertThat(registry.streamCountByStockCode("005930")).isZero();
        assertThat(terminatedCount).hasValue(1);
        assertThat(terminatedStreamId).hasValue(connection.streamId());
    }

    @Test
    @DisplayName("특정 종목의 stream들에게만 SSE 이벤트를 전송한다")
    void sendToStockCode() {
        registry.register(1L, "005930", null);
        registry.register(2L, "005930", null);
        registry.register(3L, "000660", null);

        int sentCount = registry.sendToStockCode("005930", "orderbook", Map.of("stockCode", "005930"));

        assertThat(sentCount).isEqualTo(2);
        assertThat(registry.streamCount()).isEqualTo(3);
        assertThat(registry.streamCountByStockCode("005930")).isEqualTo(2);
        assertThat(registry.streamCountByStockCode("000660")).isEqualTo(1);
    }

    @Test
    @DisplayName("필수 값이 없으면 스트림 등록과 전송에 실패한다")
    void validateRequiredValues() {
        assertThatThrownBy(() -> registry.register(null, "005930", null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> registry.register(1L, " ", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> registry.sendToStockCode("005930", " ", Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
