package com.team10.backend.domain.investment.portfolio.entity;

import com.team10.backend.domain.investment.account.entity.InvestmentAccount;
import com.team10.backend.domain.investment.stock.entity.Stock;
import com.team10.backend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "investment_holdings",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_investment_holdings_account_stock",
                columnNames = {"investment_account_id", "stock_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvestmentHolding extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "investment_account_id", nullable = false)
    private InvestmentAccount investmentAccount; // 주식 게좌

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock; // 주식 종목

    @Column(nullable = false)
    private Long quantity; // 수량

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal averagePrice; // 평균가

    public static InvestmentHolding create(
            InvestmentAccount investmentAccount,
            Stock stock,
            Long quantity,
            BigDecimal averagePrice
    ) {
        InvestmentHolding holding = new InvestmentHolding();
        holding.investmentAccount = investmentAccount;
        holding.stock = stock;
        holding.quantity = quantity;
        holding.averagePrice = averagePrice;
        return holding;
    }

    // TODO : 주식 거래 시 보유 주식 수량 및 평균가 재계산
}
