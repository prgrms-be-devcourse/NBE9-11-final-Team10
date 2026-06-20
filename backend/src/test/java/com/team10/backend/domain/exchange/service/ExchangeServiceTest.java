package com.team10.backend.domain.exchange.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.team10.backend.domain.exchange.calculator.ExchangeCalculator;
import com.team10.backend.domain.exchange.dto.res.ExchangeOrderRes;
import com.team10.backend.domain.exchange.repository.CurrencyRepository;
import com.team10.backend.domain.exchange.repository.ExchangeQuoteRepository;
import com.team10.backend.domain.exchange.repository.ExchangeRateRepository;
import com.team10.backend.domain.exchange.type.ExchangeDirection;
import com.team10.backend.domain.exchange.type.ExchangeOrderStatus;
import com.team10.backend.domain.user.repository.UserRepository;
import com.team10.backend.global.idempotency.annotation.Idempotent;
import com.team10.backend.global.idempotency.type.IdempotencyOperationType;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExchangeServiceTest {

    @Mock
    private ExchangeQuoteRepository exchangeQuoteRepository;

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrencyRepository currencyRepository;

    @Mock
    private ExchangeCalculator exchangeCalculator;

    @Mock
    private ExchangeBusinessService exchangeBusinessService;

    @InjectMocks
    private ExchangeService exchangeService;

    @Test
    @DisplayName("환전 주문은 멱등성 오케스트레이터에서 비즈니스 서비스로 위임한다")
    void createExchangeOrderDelegatesToBusinessService() {
        ExchangeOrderRes expected = new ExchangeOrderRes(
                40L,
                10L,
                ExchangeDirection.KRW_TO_FOREIGN,
                ExchangeOrderStatus.COMPLETED,
                20L,
                30L,
                new BigDecimal("100000"),
                new BigDecimal("72.2826"),
                new BigDecimal("1380.000000"),
                new BigDecimal("0.002500"),
                new BigDecimal("250.0000"),
                LocalDateTime.of(2026, 6, 21, 10, 0),
                LocalDateTime.of(2026, 6, 21, 10, 0, 1)
        );
        when(exchangeBusinessService.executeExchangeOrder(1L, 10L, 20L, 30L))
                .thenReturn(expected);

        ExchangeOrderRes response = exchangeService.createExchangeOrder(
                1L,
                "exchange-order-key",
                10L,
                20L,
                30L
        );

        assertThat(response).isSameAs(expected);
        verify(exchangeBusinessService).executeExchangeOrder(1L, 10L, 20L, 30L);
    }

    @Test
    @DisplayName("환전 주문 오케스트레이터에 멱등성 설정이 적용되어 있다")
    void createExchangeOrderHasIdempotentAnnotation() throws NoSuchMethodException {
        Method method = ExchangeService.class.getMethod(
                "createExchangeOrder",
                Long.class,
                String.class,
                Long.class,
                Long.class,
                Long.class
        );

        Idempotent idempotent = method.getAnnotation(Idempotent.class);

        assertThat(idempotent).isNotNull();
        assertThat(idempotent.operationType()).isEqualTo(IdempotencyOperationType.EXCHANGE_ORDER);
        assertThat(idempotent.userId()).isEqualTo("#userId");
        assertThat(idempotent.key()).isEqualTo("#idempotencyKey");
        assertThat(idempotent.hashFields()).containsExactly("#exchangeQuoteId", "#krwAccountId", "#fxWalletId");
    }
}
