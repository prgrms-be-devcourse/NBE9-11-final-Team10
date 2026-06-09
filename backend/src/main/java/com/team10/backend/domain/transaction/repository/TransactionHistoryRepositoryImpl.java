package com.team10.backend.domain.transaction.repository;

import static com.team10.backend.domain.transaction.service.TransactionHistoryService.SORT_PROPERTY_TRANSACTED_AT;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team10.backend.domain.transaction.dto.req.TransactionHistorySearchReq;
import com.team10.backend.domain.transaction.dto.res.TransactionHistorySearchRes;
import com.team10.backend.domain.transaction.entity.QTransactionHistory;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
public class TransactionHistoryRepositoryImpl implements TransactionHistoryRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<TransactionHistorySearchRes> search(
            Long accountId,
            TransactionHistorySearchReq filter,
            Pageable pageable
    ) {
        QTransactionHistory transactionHistory = QTransactionHistory.transactionHistory;

        BooleanBuilder condition = new BooleanBuilder();

        // where account_id = ?
        condition.and(transactionHistory.account.id.eq(accountId));

        // where transacted_at >= startDate 00:00:00
        if (filter.startDate() != null) {
            condition.and(transactionHistory.transactedAt.goe(filter.startDate().atStartOfDay()));
        }

        // where transacted_at < endDate+1 00:00:00
        if (filter.endDate() != null) {
            condition.and(transactionHistory.transactedAt.lt(filter.endDate().plusDays(1).atStartOfDay()));
        }

        // where direction = IN | OUT
        if (filter.direction() != null) {
            condition.and(transactionHistory.direction.eq(filter.direction()));
        }

        // where amount >= minAmount
        if (filter.minAmount() != null) {
            condition.and(transactionHistory.amount.goe(filter.minAmount()));
        }

        // where amount <= maxAmount
        if (filter.maxAmount() != null) {
            condition.and(transactionHistory.amount.loe(filter.maxAmount()));
        }

        // where counterpartyName = ?
        if (StringUtils.hasText(filter.counterpartyName())) {
            condition.and(transactionHistory.counterpartyName.containsIgnoreCase(filter.counterpartyName()));
        }

        // controller 단의 요청파라미터 입력 시점에서 이미 default 값 지정되므로 non-null
        Sort.Direction direction = pageable.getSort()
                .getOrderFor(SORT_PROPERTY_TRANSACTED_AT)
                .getDirection();
        OrderSpecifier<LocalDateTime> orderSpecifier = direction.isAscending()
                ? transactionHistory.transactedAt.asc()
                : transactionHistory.transactedAt.desc();

        // 동적 쿼리 완성
        List<TransactionHistorySearchRes> content = queryFactory
                .select(Projections.constructor(
                        TransactionHistorySearchRes.class,
                        transactionHistory.id,
                        transactionHistory.counterpartyName,
                        transactionHistory.amount,
                        transactionHistory.balanceAfter,
                        transactionHistory.transactedAt,
                        transactionHistory.direction
                ))
                .from(transactionHistory)
                .where(condition)
                .orderBy(orderSpecifier)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(transactionHistory.count())
                .from(transactionHistory)
                .where(condition)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }
}
