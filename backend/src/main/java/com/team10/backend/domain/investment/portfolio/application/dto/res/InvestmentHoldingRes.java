package com.team10.backend.domain.investment.portfolio.application.dto.res;

import com.team10.backend.domain.investment.portfolio.domain.entity.InvestmentHolding;
import com.team10.backend.domain.investment.stock.domain.entity.Stock;
import com.team10.backend.domain.investment.stock.domain.type.StockMarket;
import com.team10.backend.domain.investment.stock.domain.type.StockStatus;
import java.math.BigDecimal;

public record InvestmentHoldingRes(
        Long id,
        Long stockId,
        String stockCode,
        String stockName,
        StockMarket market,
        StockStatus status,
        Long quantity,
        BigDecimal averagePrice,
        Long marketCap,
        Long previousVolume
) {
    public static InvestmentHoldingRes from(InvestmentHolding holding) {
        Stock stock = holding.getStock();
        return new InvestmentHoldingRes(
                holding.getId(),
                stock.getId(),
                stock.getStockCode(),
                stock.getStockName(),
                stock.getMarket(),
                stock.getStatus(),
                holding.getQuantity(),
                holding.getAveragePrice(),
                stock.getMarketCap(),
                stock.getPreviousVolume()
        );
    }
}
