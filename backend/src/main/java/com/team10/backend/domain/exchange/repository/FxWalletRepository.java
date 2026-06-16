package com.team10.backend.domain.exchange.repository;

import com.team10.backend.domain.exchange.entity.FxWallet;
import com.team10.backend.domain.exchange.type.CurrencyCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FxWalletRepository extends JpaRepository<FxWallet, Long> {

    Optional<FxWallet> findByUserIdAndCurrencyCurrencyCode(Long userId, CurrencyCode currencyCode);

    List<FxWallet> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<FxWallet> findByIdAndUserId(Long fxWalletId, Long userId);
}
