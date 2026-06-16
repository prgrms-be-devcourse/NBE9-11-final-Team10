package com.team10.backend.domain.investment.order.entity;

import com.team10.backend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "investment_order_executions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvestmentOrderExecution extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "investment_order_id", nullable = false)
    private InvestmentOrder order;

    @Column(nullable = false)
    private Long executionPrice; // 체결 단가

    @Column(nullable = false)
    private Long executionQuantity; // 체결 수량

    public static InvestmentOrderExecution create(
            InvestmentOrder order,
            Long executionPrice,
            Long executionQuantity
    ) {
        InvestmentOrderExecution execution = new InvestmentOrderExecution();
        execution.order = order;
        execution.executionPrice = executionPrice;
        execution.executionQuantity = executionQuantity;
        return execution;
    }
}
