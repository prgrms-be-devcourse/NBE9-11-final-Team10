package com.team10.backend.domain.investment.stock.service;

import com.team10.backend.domain.investment.exception.InvestmentErrorCode;
import com.team10.backend.domain.investment.stock.dto.res.StockDetailRes;
import com.team10.backend.domain.investment.stock.dto.res.StockSummaryRes;
import com.team10.backend.domain.investment.stock.entity.Stock;
import com.team10.backend.domain.investment.stock.repository.StockRepository;
import com.team10.backend.domain.investment.stock.type.StockMarket;
import com.team10.backend.domain.investment.stock.type.StockSortType;
import com.team10.backend.domain.investment.stock.type.StockStatus;
import com.team10.backend.global.exception.BusinessException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockQueryService {

    private static final int MIN_SEARCH_KEYWORD_LENGTH = 2;

    private final StockRepository stockRepository;

    public List<StockSummaryRes> search(String keyword, StockMarket market) {
        String normalizedKeyword = normalizeKeyword(keyword);
        if (!StringUtils.hasText(normalizedKeyword)
                || normalizedKeyword.length() < MIN_SEARCH_KEYWORD_LENGTH) {
            return List.of();
        }

        return stockRepository.search(
                        normalizedKeyword,
                        market,
                        StockStatus.ACTIVE
                ).stream()
                .map(StockSummaryRes::from)
                .toList();
    }

    public Page<StockSummaryRes> getStocks(
            StockMarket market,
            StockStatus status,
            StockSortType sort,
            Sort.Direction direction,
            int page,
            int size
    ) {
        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                getSort(sort, direction)
        );

        return stockRepository.findAllByMarketAndStatus(market, status, pageRequest)
                .map(StockSummaryRes::from);
    }

    public StockDetailRes getStock(String stockCode) {
        Stock stock = stockRepository.findByStockCodeAndStatus(stockCode, StockStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(InvestmentErrorCode.STOCK_NOT_FOUND));

        return StockDetailRes.from(stock);
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim();
    }

    private Sort getSort(StockSortType sort, Sort.Direction direction) {
        Sort result = Sort.by(direction, sort.property());

        if (sort != StockSortType.STOCK_NAME) {
            result = result.and(Sort.by(Sort.Direction.ASC, StockSortType.STOCK_NAME.property()));
        }

        if (sort != StockSortType.STOCK_CODE) {
            result = result.and(Sort.by(Sort.Direction.ASC, StockSortType.STOCK_CODE.property()));
        }

        return result;
    }
}
