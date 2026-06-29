package com.team10.backend.domain.exchange.domain.repository;

import com.team10.backend.domain.exchange.domain.entity.ExchangeQuote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeQuoteRepository extends JpaRepository<ExchangeQuote, Long> {
}
