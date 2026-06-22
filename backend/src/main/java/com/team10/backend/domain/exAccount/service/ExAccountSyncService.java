package com.team10.backend.domain.exAccount.service;

import com.team10.backend.domain.codef.exAccount.dto.internal.CodefExAccountSnapshot;
import com.team10.backend.domain.codef.exAccount.store.CodefExAccountCandidateStore;
import com.team10.backend.domain.exAccount.dto.req.ExAccountLinkReq;
import com.team10.backend.domain.exAccount.dto.res.ExAccountRes;
import com.team10.backend.domain.exAccount.entity.ExAccount;
import com.team10.backend.domain.exAccount.exception.ExAccountErrorCode;
import com.team10.backend.domain.exAccount.repository.ExAccountRepository;
import com.team10.backend.domain.user.entity.User;
import com.team10.backend.domain.user.exception.UserErrorCode;
import com.team10.backend.domain.user.repository.UserRepository;
import com.team10.backend.global.exception.BusinessException;
import com.team10.backend.global.exception.GlobalErrorCode;
import com.team10.backend.global.security.HmacSha256Hasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExAccountSyncService {

    private static final int MAX_VISIBLE_PREFIX_LENGTH = 6;
    private static final int MAX_VISIBLE_SUFFIX_LENGTH = 4;
    private static final int MIN_MASK_LENGTH = 3;

    private final ExAccountRepository exAccountRepository;
    private final UserRepository userRepository;
    private final HmacSha256Hasher hmacSha256Hasher;
    private final CodefExAccountCandidateStore candidateStore;

    @Transactional
    public List<ExAccountRes> linkAccounts(Long userId, ExAccountLinkReq request) {
        if (request == null || request.candidateToken() == null || request.candidateToken().isBlank()) {
            throw new BusinessException(GlobalErrorCode.INVALID_INPUT_VALUE);
        }

        List<CodefExAccountSnapshot> snapshots = candidateStore.get(userId, request.candidateToken());
        if (snapshots.isEmpty()) {
            throw new BusinessException(ExAccountErrorCode.EX_ACCOUNT_CANDIDATE_NOT_FOUND);
        }

        List<ExAccountRes> results = new ArrayList<>();
        for (int index : request.selectedIndexes()) {
            if (index < 0 || index >= snapshots.size()) {
                throw new BusinessException(ExAccountErrorCode.EX_ACCOUNT_CANDIDATE_INVALID_INDEX);
            }
            CodefExAccountSnapshot snapshot = snapshots.get(index);
            ExAccount account = upsertAccount(userId, snapshot);
            results.add(ExAccountRes.from(account));
        }

        // 연동 성공 후 토큰 즉시 파기
        candidateStore.remove(userId, request.candidateToken());

        return results;
    }

    public String getMaskedAccountNumber(String accountNumber) {
        return maskAccountNumber(normalizeAccountNumber(accountNumber));
    }

    public String getAccountNumberHash(String accountNumber) {
        return hmacSha256Hasher.hash(normalizeAccountNumber(accountNumber));
    }

    private ExAccount upsertAccount(Long userId, CodefExAccountSnapshot snapshot) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        ProtectedAccountNumber accountNumber = protectAccountNumber(snapshot.accountNumber());

        ExAccount exAccount = exAccountRepository
                .findByUserIdAndOrganizationAndAccountNumberHash(
                        userId,
                        snapshot.organization(),
                        accountNumber.hash()
                )
                .orElse(null);

        if (exAccount == null) {
            ExAccount newAccount = ExAccount.create(
                    user,
                    snapshot.organization(),
                    accountNumber.hash(),
                    accountNumber.masked(),
                    snapshot.accountName(),
                    snapshot.accountAlias(),
                    snapshot.assetType(),
                    snapshot.balance(),
                    snapshot.withdrawableAmount(),
                    snapshot.openedAt(),
                    snapshot.maturityAt(),
                    snapshot.lastTransactionAt()
            );
            return exAccountRepository.save(newAccount);
        }

        exAccount.updateSnapshot(
                snapshot.accountName(),
                snapshot.accountAlias(),
                snapshot.balance(),
                snapshot.withdrawableAmount(),
                snapshot.maturityAt(),
                snapshot.lastTransactionAt()
        );
        return exAccount;
    }

    private ProtectedAccountNumber protectAccountNumber(String accountNumber) {
        String normalized = normalizeAccountNumber(accountNumber);
        return new ProtectedAccountNumber(
                hmacSha256Hasher.hash(normalized),
                maskAccountNumber(normalized)
        );
    }

    private String normalizeAccountNumber(String accountNumber) {
        return accountNumber.replace(" ", "").replace("-", "");
    }

    private String maskAccountNumber(String accountNumber) {
        int length = accountNumber.length();
        if (length <= MAX_VISIBLE_SUFFIX_LENGTH) {
            return "*".repeat(length);
        }

        int suffixLength = Math.min(MAX_VISIBLE_SUFFIX_LENGTH, length - MIN_MASK_LENGTH);
        int prefixLength = Math.min(
                MAX_VISIBLE_PREFIX_LENGTH,
                length - suffixLength - MIN_MASK_LENGTH
        );
        String prefix = accountNumber.substring(0, prefixLength);
        String suffix = accountNumber.substring(length - suffixLength);
        int maskLength = length - prefixLength - suffixLength;

        return prefix + "*".repeat(maskLength) + suffix;
    }

    private record ProtectedAccountNumber(String hash, String masked) {
    }
}
