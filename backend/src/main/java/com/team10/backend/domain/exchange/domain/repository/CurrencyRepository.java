package com.team10.backend.domain.exchange.domain.repository;


import com.team10.backend.domain.exchange.domain.entity.Currency;
import com.team10.backend.domain.exchange.domain.type.CurrencyCode;
import com.team10.backend.domain.exchange.domain.type.CurrencyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CurrencyRepository extends JpaRepository<Currency, Long> {

    Optional<Currency> findByCurrencyCode(CurrencyCode currencyCode);

    List<Currency> findAllByCurrencyCodeInAndStatus(Collection<CurrencyCode> currencyCodes, CurrencyStatus status);
}
