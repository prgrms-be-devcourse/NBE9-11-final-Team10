package com.team10.backend.domain.exchange.repository;

import com.team10.backend.domain.exchange.entity.ExchangeRate;
import com.team10.backend.domain.exchange.type.CurrencyCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    Optional<ExchangeRate> findByCurrencyCurrencyCode(CurrencyCode currencyCode);

    @Query("""
            select er
            from ExchangeRate er
            join fetch er.currency
            order by er.currency.currencyCode asc
    """)
    List<ExchangeRate> findAllByOrderByCurrencyCurrencyCodeAsc();

}
