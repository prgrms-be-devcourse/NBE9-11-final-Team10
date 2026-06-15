package com.team10.backend.domain.exchange.repository;

import com.team10.backend.domain.exchange.entity.ExchangeOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeOrderRepository extends JpaRepository<ExchangeOrder, Long> {
}
