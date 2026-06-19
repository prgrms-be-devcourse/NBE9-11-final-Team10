package com.team10.backend.domain.exAccount.service;

import com.team10.backend.domain.exAccount.Type.ExAccountType;
import com.team10.backend.domain.exAccount.dto.req.ExAccountLinkReq;
import com.team10.backend.domain.exAccount.dto.res.ExAccountCandidateRes;
import com.team10.backend.domain.exAccount.dto.res.ExAccountRes;
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

    public List<ExAccountCandidateRes> getLinkCandidates(Long userId, List<ExAccountLinkReq> requests) {
        List<ExAccountSyncItem> items = requests == null
                ? null
                : requests.stream()
                .map(ExAccountLinkReq::toSyncItem)
                .toList();

        validateItems(items);

        return items.stream()
                .peek(this::validateItem)
                .map(item -> ExAccountCandidateRes.from(item, isAlreadyLinked(userId, item)))
                .toList();
    }

    public ExAccountRes linkAccount(Long userId, ExAccountLinkReq request) {
        ExAccountSyncItem item = request == null ? null : request.toSyncItem();
        validateItem(item);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        ExAccount account = upsertAccount(user, item);
        return ExAccountRes.from(account);
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

    private boolean isAlreadyLinked(Long userId, ExAccountSyncItem item) {
        return exAccountRepository
                .findByUserIdAndOrganizationAndAccountNumber(
                        userId,
                        item.organization(),
                        item.accountNumber()
                )
                .isPresent();
    }

    private ExAccount upsertAccount(User user, ExAccountSyncItem item) {
        ExAccount exAccount = exAccountRepository
                .findByUserIdAndOrganizationAndAccountNumber(
                        user.getId(),
                        item.organization(),
                        item.accountNumber()
                )
                .orElse(null);

        if (exAccount == null) {
            return exAccountRepository.save(item.toEntity(user));
        }

        exAccount.updateSnapshot(
                item.accountName(),
                item.accountAlias(),
                item.balance(),
                item.withdrawableAmount(),
                item.maturityAt(),
                item.lastTransactionAt()
        );
        return exAccount;
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

}
