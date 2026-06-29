package com.team10.backend.domain.transaction.domain.repository;

import com.team10.backend.domain.transaction.domain.entity.TransactionHistory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionHistoryRepository
        extends JpaRepository<TransactionHistory, Long>, TransactionHistoryRepositoryCustom {

    Optional<TransactionHistory> findByIdAndAccountId(Long transactionId, Long accountId);
}
