package com.team10.backend.domain.exchange.repository;

import com.team10.backend.domain.exchange.entity.ExchangeRate;
import com.team10.backend.domain.exchange.type.CurrencyCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    @Query("""
        select er
        from ExchangeRate er
        where er.currency.currencyCode = :currencyCode
          and er.rateAt = :rateAt
        """)
    Optional<ExchangeRate> findByCurrencyCodeAndRateAt(CurrencyCode currencyCode, LocalDateTime rateAt);

    @Query("""
        select er
        from ExchangeRate er
        join fetch er.currency c
        where c.currencyCode = :currencyCode
        order by er.rateAt desc
        limit 1
        """)
    Optional<ExchangeRate> findLatestByCurrencyCode(CurrencyCode currencyCode);

    @Query("""
        select er
        from ExchangeRate er
        join fetch er.currency c
        where er.rateAt = :rateAt
        order by c.currencyCode asc
        """)
    List<ExchangeRate> findAllByRateAtOrderByCurrencyCodeAsc(LocalDateTime rateAt);

    @Query("select max(er.rateAt) from ExchangeRate er")
    Optional<LocalDateTime> findLatestRateAt();
}
