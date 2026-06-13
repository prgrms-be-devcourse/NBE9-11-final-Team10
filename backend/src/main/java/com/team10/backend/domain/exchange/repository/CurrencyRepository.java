package com.team10.backend.domain.exchange.repository;

import com.team10.backend.domain.exchange.entity.Currency;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurrencyRepository extends JpaRepository<Currency, Long> {
}
