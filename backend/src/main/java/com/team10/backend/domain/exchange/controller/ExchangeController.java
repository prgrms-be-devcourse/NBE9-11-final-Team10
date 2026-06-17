package com.team10.backend.domain.exchange.controller;

import com.team10.backend.domain.exchange.dto.req.ExchangeQuoteCreateReq;
import com.team10.backend.domain.exchange.dto.res.CurrencyRes;
import com.team10.backend.domain.exchange.dto.res.ExchangeQuoteRes;
import com.team10.backend.domain.exchange.dto.res.ExchangeRateRes;
import com.team10.backend.domain.exchange.entity.Currency;
import com.team10.backend.domain.exchange.service.ExchangeRateService;
import com.team10.backend.domain.exchange.service.ExchangeService;
import com.team10.backend.domain.exchange.type.CurrencyCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/exchanges")
@Tag(name = "ExchangeController", description = "환전 API")
@RequiredArgsConstructor
public class ExchangeController {

    private final ExchangeRateService exchangeRateService;
    private final ExchangeService exchangeService;

    @GetMapping("/currencies")
    @Operation(description = "지원 통화 목록 조회")
    public ResponseEntity<List<CurrencyRes>> getCurrencies() {
        List<CurrencyRes> response = exchangeRateService.getCurrencies();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rates")
    @Operation(description = "현재 환율 목록 조회")
    public ResponseEntity<List<ExchangeRateRes>> getExchangeRates() {
        return ResponseEntity.ok(exchangeRateService.getLatestRates());
    }

    @GetMapping("/currencies/{currencyCode}")
    @Operation(description = "특정 통화 환율 조회")
    public ResponseEntity<ExchangeRateRes> getExchangeRate(
            @PathVariable CurrencyCode currencyCode
            ) {
        return ResponseEntity.ok(exchangeRateService.getLatestRate(currencyCode));
    }

    @PostMapping("/currencies/quotes")
    @Operation(description = "환전 견적 생성")
    public ResponseEntity<ExchangeQuoteRes> createExchangeQuote(
            // TODO: 인증된 사용자(@AuthenticationPrincipal)에서 userId 꺼내쓰도록 수정
            @RequestParam Long userId,
            @Valid @RequestBody ExchangeQuoteCreateReq request
    ) {
        ExchangeQuoteRes response = exchangeService.createQuote(
                userId,
                request.fromCurrencyCode(),
                request.toCurrencyCode(),
                request.fromAmount());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
