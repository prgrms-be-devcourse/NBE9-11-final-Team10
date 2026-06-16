package com.team10.backend.domain.investment.order.entity;

import com.team10.backend.domain.investment.account.entity.InvestmentAccount;
import com.team10.backend.domain.investment.order.type.InvestmentOrderStatus;
import com.team10.backend.domain.investment.order.type.InvestmentPriceType;
import com.team10.backend.domain.investment.order.type.InvestmentTradeType;
import com.team10.backend.domain.investment.stock.entity.Stock;
import com.team10.backend.global.entity.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "investment_orders",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_investment_orders_account_idempotency",
                columnNames = {"investment_account_id", "idempotency_key"} // 계좌번호와 키 복합 유니크로 중복 주문 방지
        )
)
@AttributeOverride(name = "id", column = @Column(name = "investment_order_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvestmentOrder extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "investment_account_id", nullable = false)
    private InvestmentAccount investmentAccount; // 주문 주식 계좌

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock; // 대상 주식 종목

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private InvestmentTradeType tradeType; // 매수 | 매도

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private InvestmentPriceType priceType; // 시장가 | 지정가

    @Column(nullable = false)
    private Long quantity; // 주문 수량

    @Column(nullable = false)
    private Long remainingQuantity; // 지정가 잔여 주문 수량 스냅샷

    private Long orderPrice; // 지정가 주문의 주문 요청가

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InvestmentOrderStatus status; // 주문 상태 : 등록 , 부분 체결 , 완전 체결 , 취소

    @Column(nullable = false)
    private UUID idempotencyKey; // 중복 주문 방지 키 ( UUID )

    public static InvestmentOrder create(
            InvestmentAccount investmentAccount,
            Stock stock,
            InvestmentTradeType tradeType,
            InvestmentPriceType priceType,
            Long quantity,
            Long orderPrice,
            UUID idempotencyKey
    ) {
        InvestmentOrder order = new InvestmentOrder();
        order.investmentAccount = investmentAccount;
        order.stock = stock;
        order.tradeType = tradeType;
        order.priceType = priceType;
        order.quantity = quantity;
        order.remainingQuantity = quantity;
        order.orderPrice = orderPrice;
        order.status = InvestmentOrderStatus.PENDING;
        order.idempotencyKey = idempotencyKey;
        return order;
    }

    public boolean isModifiable() {
        return status == InvestmentOrderStatus.PENDING || status == InvestmentOrderStatus.PARTIALLY_FILLED;
    }

}
