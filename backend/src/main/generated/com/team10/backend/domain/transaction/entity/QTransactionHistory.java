package com.team10.backend.domain.transaction.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.annotations.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QTransactionHistory is a Querydsl query type for TransactionHistory
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTransactionHistory extends EntityPathBase<TransactionHistory> {

    private static final long serialVersionUID = 704678598L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QTransactionHistory transactionHistory = new QTransactionHistory("transactionHistory");

    public final com.team10.backend.global.entity.QBaseEntity _super = new com.team10.backend.global.entity.QBaseEntity(this);

    public final com.team10.backend.domain.account.entity.QAccount account;

    public final NumberPath<Long> amount = createNumber("amount", Long.class);

    public final NumberPath<Long> balanceAfter = createNumber("balanceAfter", Long.class);

    public final NumberPath<Long> balanceBefore = createNumber("balanceBefore", Long.class);

    public final StringPath counterpartyAccountNumber = createString("counterpartyAccountNumber");

    public final StringPath counterpartyName = createString("counterpartyName");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final EnumPath<com.team10.backend.domain.transaction.type.TransactionDirection> direction = createEnum("direction", com.team10.backend.domain.transaction.type.TransactionDirection.class);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath memo = createString("memo");

    public final DateTimePath<java.time.LocalDateTime> transactedAt = createDateTime("transactedAt", java.time.LocalDateTime.class);

    public final com.team10.backend.domain.transfer.entity.QTransfer transfer;

    public final EnumPath<com.team10.backend.domain.transaction.type.TransactionType> type = createEnum("type", com.team10.backend.domain.transaction.type.TransactionType.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QTransactionHistory(String variable) {
        this(TransactionHistory.class, forVariable(variable), INITS);
    }

    public QTransactionHistory(Path<? extends TransactionHistory> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QTransactionHistory(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QTransactionHistory(PathMetadata metadata, PathInits inits) {
        this(TransactionHistory.class, metadata, inits);
    }

    public QTransactionHistory(Class<? extends TransactionHistory> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.account = inits.isInitialized("account") ? new com.team10.backend.domain.account.entity.QAccount(forProperty("account"), inits.get("account")) : null;
        this.transfer = inits.isInitialized("transfer") ? new com.team10.backend.domain.transfer.entity.QTransfer(forProperty("transfer"), inits.get("transfer")) : null;
    }

}

