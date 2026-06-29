package com.team10.backend.domain.investment.stock.application.dto.res;

import com.team10.backend.domain.investment.stock.domain.entity.Stock;
import com.team10.backend.domain.investment.stock.domain.type.StockMarket;
import com.team10.backend.domain.investment.stock.domain.type.StockStatus;
import com.team10.backend.domain.investment.domain.type.CurrencyCode;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record StockDetailRes(
        Long id,
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
        Long previousVolume,
        LocalDateTime updatedAt
) {

    public static StockDetailRes from(Stock stock) {
        return new StockDetailRes(
                stock.getId(),
                stock.getStockCode(),
                stock.getStandardCode(),
                stock.getStockName(),
                stock.getMarket(),
                stock.getCurrencyCode(),
                stock.getStatus(),
                stock.getListedDate(),
                stock.getCapitalAmount(),
                stock.getSalesAmount(),
                stock.getNetIncome(),
                stock.getMarketCap(),
                stock.getPreviousVolume(),
                stock.getUpdatedAt()
        );
    }
}
