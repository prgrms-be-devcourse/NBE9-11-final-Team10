package com.team10.backend.domain.exAccount.service;

import com.team10.backend.domain.exAccount.Type.ExAccountType;
import com.team10.backend.domain.exAccount.entity.ExAccount;
import com.team10.backend.domain.exAccount.exception.ExAccountErrorCode;
import com.team10.backend.domain.exAccount.repository.ExAccountRepository;
import com.team10.backend.domain.user.entity.User;
import com.team10.backend.domain.user.exception.UserErrorCode;
import com.team10.backend.domain.user.repository.UserRepository;
import com.team10.backend.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.util.StringUtils.hasText;

@Service
@RequiredArgsConstructor
public class ExAccountSyncService {

    private final ExAccountRepository exAccountRepository;
    private final UserRepository userRepository;

    public ExAccountSyncResult syncAccount(Long userId, List<ExAccountSyncItem> items) {

        validateItems(items); //외부에서 계좌 목록 넘겼는지 확인

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        int createdCount = 0;
        int updatedCount = 0;

        for (ExAccountSyncItem item : items) {
            validateItem(item);

            if (upsertAccount(user, item)) {
                createdCount++;
                continue;
            }

            updatedCount++;
        }

        return new ExAccountSyncResult(items.size(), createdCount, updatedCount);
    }

    private void validateItems(List<ExAccountSyncItem> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException(ExAccountErrorCode.EX_ACCOUNT_SYNC_ITEMS_REQUIRED);
        }
    }


    private void validateItem(ExAccountSyncItem item) {
        if (item == null
                || !hasText(item.organization())
                || !hasText(item.accountNumber())
                || !hasText(item.accountName())
                || item.assetType() == null) {
            throw new BusinessException(ExAccountErrorCode.EX_ACCOUNT_SYNC_REQUIRED_FIELD_MISSING);
        }
    }

    private boolean upsertAccount(User user, ExAccountSyncItem item) {
        // 4. 같은 사용자/기관/계좌번호 기준으로 기존 외부 계좌를 찾는다.
        ExAccount exAccount = exAccountRepository
                .findByUserIdAndOrganizationAndAccountNumber(
                        user.getId(),
                        item.organization(),
                        item.accountNumber()
                )
                .orElse(null);

        // 계좌가 없으면 신규 외부 계좌로 저장
        if (exAccount == null) {
            exAccountRepository.save(item.toEntity(user));
            return true;
        }

        // 스냅샷만 갱신
        exAccount.updateSnapshot(
                item.accountName(),
                item.accountAlias(),
                item.balance(),
                item.withdrawableAmount(),
                item.maturityAt(),
                item.lastTransactionAt()
        );
        return false;
    }



    public record ExAccountSyncItem(
            String organization,
            String accountNumber,
            String accountName,
            String accountAlias,
            ExAccountType assetType,
            BigDecimal balance,
            BigDecimal withdrawableAmount,
            LocalDate openedAt,
            LocalDate maturityAt,
            LocalDate lastTransactionAt
    ) {

        // 신규 저장이 필요할 때 ExAccount.create(...)
        private ExAccount toEntity(User user) {
            return ExAccount.create(
                    user,
                    organization,
                    accountNumber,
                    accountName,
                    accountAlias,
                    assetType,
                    balance,
                    withdrawableAmount,
                    openedAt,
                    maturityAt,
                    lastTransactionAt
            );
        }
    }


    public record ExAccountSyncResult(
            int requestedCount, //요청 갯수
            int createdCount, //신규 저장 갯수
            int updatedCount //업데이트 저장 갯수
    ) {
    }
}
