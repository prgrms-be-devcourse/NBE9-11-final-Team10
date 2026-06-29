package com.team10.backend.domain.transfer.domain.repository;

import com.team10.backend.domain.transfer.domain.entity.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferRepository extends JpaRepository<Transfer, Long> {
}
