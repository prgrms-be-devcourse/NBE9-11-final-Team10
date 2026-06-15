package com.team10.backend.domain.exchange.entity;

import com.team10.backend.domain.exchange.type.FxWalletStatus;
import com.team10.backend.domain.user.entity.User;
import com.team10.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Entity
@Table(
        name = "fx_wallets",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_fx_wallets_user_currency",
                        columnNames = {"user_id", "currency_code"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FxWallet extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY) // 기본적으로 @ManyToOne은 참조 대상 엔터티의 PK를 참조
    @JoinColumn(name = "currency_code", referencedColumnName = "currency_code", nullable = false) // UK인 currency_code 를 참조하므로 명시필요
    private Currency currency;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FxWalletStatus status;

    public static FxWallet create(User user, Currency currency) {
        FxWallet fxWallet = new FxWallet();
        fxWallet.user = user;
        fxWallet.currency = currency;
        fxWallet.balance = BigDecimal.ZERO;
        fxWallet.status = FxWalletStatus.ACTIVE;
        return fxWallet;
    }

    public void close() {
        this.status = FxWalletStatus.CLOSED;
    }

    public boolean isActive() {
        return this.status == FxWalletStatus.ACTIVE;
    }

    public boolean hasBalance() {
        return this.balance.compareTo(BigDecimal.ZERO) != 0;
    }
}
