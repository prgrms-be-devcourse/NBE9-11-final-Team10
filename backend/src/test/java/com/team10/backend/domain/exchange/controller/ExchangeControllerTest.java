package com.team10.backend.domain.exchange.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team10.backend.domain.exchange.dto.req.ExchangeQuoteCreateReq;
import com.team10.backend.domain.exchange.dto.res.ExchangeQuoteRes;
import com.team10.backend.domain.exchange.service.ExchangeRateService;
import com.team10.backend.domain.exchange.service.ExchangeService;
import com.team10.backend.domain.exchange.type.CurrencyCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ExchangeController.class)
class ExchangeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ExchangeRateService exchangeRateService;

    @MockitoBean
    private ExchangeService exchangeService;

    @Test
    @DisplayName("환전 견적 생성 API는 userId와 요청 본문을 받아 201을 반환한다")
    void createExchangeQuote() throws Exception {
        ExchangeQuoteCreateReq request = new ExchangeQuoteCreateReq(
                CurrencyCode.KRW,
                CurrencyCode.USD,
                new BigDecimal("100000")
        );
        ExchangeQuoteRes response = new ExchangeQuoteRes(
                1L,
                CurrencyCode.KRW,
                CurrencyCode.USD,
                new BigDecimal("100000"),
                new BigDecimal("1380.000000"),
                new BigDecimal("0.002500"),
                new BigDecimal("250"),
                new BigDecimal("72.2826"),
                LocalDateTime.of(2026, 6, 16, 15, 5),
                LocalDateTime.of(2026, 6, 16, 15, 0)
        );

        when(exchangeService.createQuote(
                eq(1L),
                eq(CurrencyCode.KRW),
                eq(CurrencyCode.USD),
                eq(new BigDecimal("100000"))
        )).thenReturn(response);

        mockMvc.perform(post("/api/v1/exchanges/currencies/quotes")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exchangeQuoteId").value(1L))
                .andExpect(jsonPath("$.fromCurrency").value("KRW"))
                .andExpect(jsonPath("$.toCurrency").value("USD"))
                .andExpect(jsonPath("$.fromAmount").value(100000))
                .andExpect(jsonPath("$.rate").value(1380.000000))
                .andExpect(jsonPath("$.feeRate").value(0.002500))
                .andExpect(jsonPath("$.fee").value(250))
                .andExpect(jsonPath("$.expectedToAmount").value(72.2826));

        verify(exchangeService).createQuote(
                eq(1L),
                eq(CurrencyCode.KRW),
                eq(CurrencyCode.USD),
                eq(new BigDecimal("100000"))
        );
    }

    @Test
    @DisplayName("환전 견적 생성 API는 출금 통화가 없으면 400을 반환한다")
    void createExchangeQuoteWithoutFromCurrency() throws Exception {
        String request = """
                {
                  "toCurrencyCode": "USD",
                  "fromAmount": 100000
                }
                """;

        mockMvc.perform(post("/api/v1/exchanges/currencies/quotes")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("환전 견적 생성 API는 환전 금액이 0 이하이면 400을 반환한다")
    void createExchangeQuoteWithNonPositiveAmount() throws Exception {
        ExchangeQuoteCreateReq request = new ExchangeQuoteCreateReq(
                CurrencyCode.KRW,
                CurrencyCode.USD,
                BigDecimal.ZERO
        );

        mockMvc.perform(post("/api/v1/exchanges/currencies/quotes")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("환전 견적 생성 API는 userId가 없으면 400을 반환한다")
    void createExchangeQuoteWithoutUserId() throws Exception {
        ExchangeQuoteCreateReq request = new ExchangeQuoteCreateReq(
                CurrencyCode.KRW,
                CurrencyCode.USD,
                new BigDecimal("100000")
        );

        mockMvc.perform(post("/api/v1/exchanges/currencies/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
