package com.team10.backend.domain.exchange.dto.res;

import com.team10.backend.domain.exchange.entity.Currency;
import com.team10.backend.domain.exchange.type.CurrencyCode;
import com.team10.backend.domain.exchange.type.CurrencyStatus;

public record CurrencyRes(
        Long currencyId,
        CurrencyCode currencyCode,
        String currencyName,
        String countryName,
        Integer decimalPlaces,
        CurrencyStatus status
) {

    public static CurrencyRes from(Currency currency) {
        return new CurrencyRes(
                currency.getId(),
                currency.getCurrencyCode(),
                currency.getCurrencyName(),
                currency.getCountryName(),
                currency.getDecimalPlaces(),
                currency.getStatus()
        );
    }
}
