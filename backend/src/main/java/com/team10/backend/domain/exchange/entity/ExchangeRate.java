package com.team10.backend.domain.exchange.entity;

import com.team10.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "exchange_rates")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExchangeRate extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_code", referencedColumnName = "currency_code", nullable = false)
    private Currency currency;

    @Column(name = "ttb", nullable = false, precision = 19, scale = 6)
    private BigDecimal ttb; // 은행이 외화를 살 때 원화 환율

    @Column(name = "tts", nullable = false, precision = 19, scale = 6)
    private BigDecimal tts; // 은행이 외화를 팔 때 원화 환율

    @Column(name = "deal_bas_r", nullable = false, precision = 19, scale = 6)
    private BigDecimal dealBasR; // 외화 기준 매매기준율

    @Column(name = "rate_at", nullable = false)
    private LocalDateTime rateAt;

    public static ExchangeRate create(
            Currency currency,
            BigDecimal ttb,
            BigDecimal tts,
            BigDecimal dealBasR,
            LocalDateTime rateAt
    ) {
        ExchangeRate exchangeRate = new ExchangeRate();
        exchangeRate.currency = currency;
        exchangeRate.ttb = ttb;
        exchangeRate.tts = tts;
        exchangeRate.dealBasR = dealBasR;
        exchangeRate.rateAt = rateAt;
        return exchangeRate;
    }
}
