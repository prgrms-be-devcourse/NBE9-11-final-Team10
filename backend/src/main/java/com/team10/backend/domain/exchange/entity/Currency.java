package com.team10.backend.domain.exchange.entity;

import com.team10.backend.domain.exchange.type.CurrencyCode;
import com.team10.backend.domain.exchange.type.CurrencyStatus;
import com.team10.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "currencies")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Currency extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "currency_code", nullable = false, unique = true, length = 3)
    private CurrencyCode currencyCode;

    @Column(name = "currency_name", nullable = false, length = 50)
    private String currencyName;

    @Column(name = "country_name", nullable = false, length = 50)
    private String countryName;

    @Column(name = "decimal_places", nullable = false)
    private Integer decimalPlaces;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CurrencyStatus status;
}
