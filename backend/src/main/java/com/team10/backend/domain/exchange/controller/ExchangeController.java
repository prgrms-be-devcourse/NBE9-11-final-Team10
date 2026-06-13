package com.team10.backend.domain.exchange.controller;

import com.team10.backend.domain.exchange.entity.Currency;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/exchanges")
@Tag(name = "ExchangeController", description = "환전 API")
@RequiredArgsConstructor
public class ExchangeController {

    @GetMapping("/currencies")
    @Operation(description = "지원 통화 목록 조회")
    public ResponseEntity<List<Currency>> getCurrencies() {
        throw new UnsupportedOperationException("구현 예정 기능");
    }

    @GetMapping("/rates")
    @Operation(description = "현재 환율 목록 조회")
    public ResponseEntity<List<Currency>> getExchangeRates() {
        throw new UnsupportedOperationException("구현 예정 기능");
    }

    @GetMapping("/currencies/{currencyCode}")
    @Operation(description = "특정 통화 환율 조회")
    public ResponseEntity<List<Currency>> getExchangeRate() {
        throw new UnsupportedOperationException("구현 예정 기능");
    }

    @PostMapping("/currencies/quotes")
    @Operation(description = "환전 견적 생성")
    public ResponseEntity<List<Currency>> createExchangeQuote() {
        throw new UnsupportedOperationException("구현 예정 기능");
    }

    @PostMapping("/currencies/orders")
    @Operation(description = "환전 주문 실행")
    public ResponseEntity<List<Currency>> createExchangeOrder() {
        throw new UnsupportedOperationException("구현 예정 기능");
    }

    @GetMapping("/currencies/orders/{exchangeOrderId}")
    @Operation(description = "환전 주문 상세 조회")
    public ResponseEntity<List<Currency>> getExchangeOrder() {
        throw new UnsupportedOperationException("구현 예정 기능");
    }

    @GetMapping("/currencies/orders")
    @Operation(description = "내 환전 주문 목록 조회")
    public ResponseEntity<List<Currency>> getExchangeOrders() {
        throw new UnsupportedOperationException("구현 예정 기능");
    }

}
