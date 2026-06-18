package com.team10.backend.domain.exAccount.repository;

import com.team10.backend.domain.exAccount.entity.ExAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExAccountRepository extends JpaRepository<ExAccount, Long> {
}
