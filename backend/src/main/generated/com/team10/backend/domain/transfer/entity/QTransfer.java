package com.team10.backend.domain.transfer.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.annotations.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QTransfer is a Querydsl query type for Transfer
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTransfer extends EntityPathBase<Transfer> {

    private static final long serialVersionUID = -19013218L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QTransfer transfer = new QTransfer("transfer");

    public final com.team10.backend.global.entity.QBaseEntity _super = new com.team10.backend.global.entity.QBaseEntity(this);

    public final NumberPath<Long> amount = createNumber("amount", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath memo = createString("memo");

    public final com.team10.backend.domain.account.entity.QAccount receiverAccount;

    public final com.team10.backend.domain.account.entity.QAccount senderAccount;

    public final EnumPath<com.team10.backend.domain.transfer.type.TransferStatus> status = createEnum("status", com.team10.backend.domain.transfer.type.TransferStatus.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QTransfer(String variable) {
        this(Transfer.class, forVariable(variable), INITS);
    }

    public QTransfer(Path<? extends Transfer> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QTransfer(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QTransfer(PathMetadata metadata, PathInits inits) {
        this(Transfer.class, metadata, inits);
    }

    public QTransfer(Class<? extends Transfer> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.receiverAccount = inits.isInitialized("receiverAccount") ? new com.team10.backend.domain.account.entity.QAccount(forProperty("receiverAccount"), inits.get("receiverAccount")) : null;
        this.senderAccount = inits.isInitialized("senderAccount") ? new com.team10.backend.domain.account.entity.QAccount(forProperty("senderAccount"), inits.get("senderAccount")) : null;
    }

}

