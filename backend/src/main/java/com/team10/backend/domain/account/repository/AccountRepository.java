package com.team10.backend.domain.account.repository;

import com.team10.backend.domain.account.entity.Account;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findAllByUserId(Long userId);

    Optional<Account> findByIdAndUserId(Long accountId, Long userId);

    boolean existsByAccountNumber(String accountNumber);
}