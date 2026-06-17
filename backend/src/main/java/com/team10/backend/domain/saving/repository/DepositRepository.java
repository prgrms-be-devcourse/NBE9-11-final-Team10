package com.team10.backend.domain.saving.repository;

import com.team10.backend.domain.saving.entity.Deposit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepositRepository extends JpaRepository<Deposit, Long> {
}
