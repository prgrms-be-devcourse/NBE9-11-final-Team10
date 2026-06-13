package com.team10.backend.domain.exchange.repository;

import com.team10.backend.domain.exchange.entity.FxWallet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FxWalletRepository extends JpaRepository<FxWallet, Long> {
}
