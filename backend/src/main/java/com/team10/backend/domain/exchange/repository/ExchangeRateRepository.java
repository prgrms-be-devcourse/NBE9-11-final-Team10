package com.team10.backend.domain.exchange.repository;

import com.team10.backend.domain.exchange.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {
}
