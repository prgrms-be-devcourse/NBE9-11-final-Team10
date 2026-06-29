package com.team10.backend.domain.investment.infrastructure.client.stock.dto;

import com.team10.backend.domain.investment.stock.domain.type.StockMarket;
import com.team10.backend.domain.investment.stock.domain.type.StockStatus;
import com.team10.backend.domain.investment.domain.type.CurrencyCode;
import java.time.LocalDate;

public record KisStockMasterRow(
        String stockCode,
        String standardCode,
        String stockName,
        StockMarket market,
        CurrencyCode currencyCode,
        StockStatus status,
        LocalDate listedDate,
        Long capitalAmount,
        Long salesAmount,
        Long netIncome,
        Long marketCap,
        Long previousVolume
) {
}
