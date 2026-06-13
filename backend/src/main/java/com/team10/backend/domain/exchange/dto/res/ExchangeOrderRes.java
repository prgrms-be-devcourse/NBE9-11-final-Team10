package com.team10.backend.domain.exchange.dto.res;

import com.team10.backend.domain.account.entity.Account;
import com.team10.backend.domain.exchange.entity.ExchangeOrder;
import com.team10.backend.domain.exchange.entity.FxWallet;
import com.team10.backend.domain.exchange.type.ExchangeDirection;
import com.team10.backend.domain.exchange.type.ExchangeOrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExchangeOrderRes(
        Long exchangeOrderId,
        Long exchangeQuoteId,
        ExchangeDirection direction,
        ExchangeOrderStatus status,
        Long krwAccountId,
        Long fxWalletId,
        BigDecimal fromAmount,
        BigDecimal toAmount,
        BigDecimal appliedRate,
        BigDecimal feeRate,
        BigDecimal fee,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {

    public static ExchangeOrderRes from(ExchangeOrder exchangeOrder) {
        return new ExchangeOrderRes(
                exchangeOrder.getId(),
                exchangeOrder.getExchangeQuote().getId(),
                exchangeOrder.getDirection(),
                exchangeOrder.getStatus(),
                getAccountId(exchangeOrder.getKrwAccount()),
                getFxWalletId(exchangeOrder.getFxWallet()),
                exchangeOrder.getFromAmount(),
                exchangeOrder.getToAmount(),
                exchangeOrder.getAppliedRate(),
                exchangeOrder.getFeeRate(),
                exchangeOrder.getFee(),
                exchangeOrder.getCreatedAt(),
                exchangeOrder.getCompletedAt()
        );
    }

    private static Long getAccountId(Account account) {
        return account == null ? null : account.getId();
    }

    private static Long getFxWalletId(FxWallet fxWallet) {
        return fxWallet == null ? null : fxWallet.getId();
    }
}
