package com.team10.backend.domain.exchange.dto.res;

import com.team10.backend.domain.exchange.entity.FxWallet;
import com.team10.backend.domain.exchange.type.CurrencyCode;
import com.team10.backend.domain.exchange.type.FxWalletStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FxWalletRes(
        Long walletId,
        CurrencyCode currencyCode,
        BigDecimal balance,
        FxWalletStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static FxWalletRes from(FxWallet fxWallet) {
        return new FxWalletRes(
                fxWallet.getId(),
                fxWallet.getCurrency().getCurrencyCode(),
                fxWallet.getBalance(),
                fxWallet.getStatus(),
                fxWallet.getCreatedAt(),
                fxWallet.getUpdatedAt()
        );
    }
}
