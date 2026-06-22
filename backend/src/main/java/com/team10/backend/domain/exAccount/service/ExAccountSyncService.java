package com.team10.backend.domain.exAccount.service;

import com.team10.backend.domain.exAccount.dto.req.ExAccountLinkReq;
import com.team10.backend.domain.exAccount.dto.res.ExAccountCandidateRes;
import com.team10.backend.domain.exAccount.dto.res.ExAccountRes;
import com.team10.backend.domain.exAccount.entity.ExAccount;
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

import java.util.List;

import static org.springframework.util.StringUtils.hasText;

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

    public List<ExAccountCandidateRes> getLinkCandidates(Long userId, List<ExAccountLinkReq> requests) {
        validateItems(requests);

        return requests.stream()
                .map(request -> toCandidate(userId, request))
                .toList();
    }

    @Transactional
    public ExAccountRes linkAccount(Long userId, ExAccountLinkReq request) {
        validateItem(request);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        ExAccount account = upsertAccount(user, request);
        return ExAccountRes.from(account);
    }

    private void validateItems(List<ExAccountLinkReq> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new BusinessException(GlobalErrorCode.INVALID_INPUT_VALUE);
        }

        requests.forEach(this::validateItem);
    }

    private void validateItem(ExAccountLinkReq request) {
        if (request == null
                || !hasText(request.organization())
                || !hasText(request.accountNumber())
                || !hasText(normalizeAccountNumber(request.accountNumber()))
                || !hasText(request.accountName())
                || request.assetType() == null) {
            throw new BusinessException(GlobalErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private ExAccountCandidateRes toCandidate(Long userId, ExAccountLinkReq request) {
        ProtectedAccountNumber accountNumber = protectAccountNumber(request.accountNumber());
        boolean linked = exAccountRepository
                .findByUserIdAndOrganizationAndAccountNumberHash(
                        userId,
                        request.organization(),
                        accountNumber.hash()
                )
                .isPresent();

        return ExAccountCandidateRes.from(request, accountNumber.masked(), linked);
    }

    private ExAccount upsertAccount(User user, ExAccountLinkReq request) {
        ProtectedAccountNumber accountNumber = protectAccountNumber(request.accountNumber());

        ExAccount exAccount = exAccountRepository
                .findByUserIdAndOrganizationAndAccountNumberHash(
                        user.getId(),
                        request.organization(),
                        accountNumber.hash()
                )
                .orElse(null);

        if (exAccount == null) {
            return exAccountRepository.save(
                    request.toEntity(user, accountNumber.hash(), accountNumber.masked())
            );
        }

        request.applyTo(exAccount);
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
