package com.team10.backend.domain.exAccount.service;

import com.team10.backend.domain.exAccount.Type.ExAccountType;
import com.team10.backend.domain.exAccount.entity.ExAccount;
import com.team10.backend.domain.user.entity.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ExAccountSyncService {

    public ExAccountSyncResult syncAccount(Long userId, List<ExAccountSyncItem> items) {

        int createdCount = 0;
        int updatedCount = 0;

        return new ExAccountSyncResult(items.size(), createdCount, updatedCount);
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
