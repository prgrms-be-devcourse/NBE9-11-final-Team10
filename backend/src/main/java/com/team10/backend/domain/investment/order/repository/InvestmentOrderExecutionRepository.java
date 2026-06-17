package com.team10.backend.domain.investment.order.repository;

import com.team10.backend.domain.investment.order.entity.InvestmentOrderExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvestmentOrderExecutionRepository extends JpaRepository<InvestmentOrderExecution, Long> {

}
