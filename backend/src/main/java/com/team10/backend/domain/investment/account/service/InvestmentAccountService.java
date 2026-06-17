package com.team10.backend.domain.investment.account.service;

import com.team10.backend.domain.investment.account.dto.req.InvestmentAccountCreateReq;
import com.team10.backend.domain.investment.account.dto.req.InvestmentAccountUpdateReq;
import com.team10.backend.domain.investment.account.dto.res.InvestmentAccountCreateRes;
import com.team10.backend.domain.investment.account.dto.res.InvestmentAccountOpenVerificationRes;
import com.team10.backend.domain.investment.account.dto.res.InvestmentAccountUpdateRes;
import com.team10.backend.domain.investment.account.entity.InvestmentAccount;
import com.team10.backend.domain.investment.account.repository.InvestmentAccountRepository;
import com.team10.backend.domain.investment.account.util.InvestmentAccountNumberGenerator;
import com.team10.backend.domain.investment.exception.InvestmentErrorCode;
import com.team10.backend.domain.user.entity.User;
import com.team10.backend.domain.user.exception.UserErrorCode;
import com.team10.backend.domain.user.repository.UserRepository;
import com.team10.backend.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvestmentAccountService {

    private static final int MAX_ACCOUNT_NUMBER_GENERATION_RETRY = 10;

    private final InvestmentAccountRepository investmentAccountRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final InvestmentAccountOpenVerificationKeyService verificationKeyService;

    public InvestmentAccountOpenVerificationRes issueOpenVerificationKey(Long userId) {
        User user = getVerifiedUser(userId);
        String verificationKey = verificationKeyService.generateAndStore(user.getId());

        return new InvestmentAccountOpenVerificationRes(
                verificationKey,
                verificationKeyService.ttlSeconds()
        );
    }

    @Transactional
    public InvestmentAccountCreateRes createAccount(Long userId, InvestmentAccountCreateReq request) {
        User user = getVerifiedUser(userId);
        String accountNumber = generateUniqueAccountNumber();

        if (!verificationKeyService.verifyAndDelete(userId, request.verificationKey())) {
            throw new BusinessException(InvestmentErrorCode.INVESTMENT_ACCOUNT_OPEN_VERIFICATION_KEY_INVALID);
        }

        InvestmentAccount account = InvestmentAccount.create(
                user,
                accountNumber,
                request.nickname(),
                passwordEncoder.encode(request.accountPassword()),
                request.currencyCode()
        );

        InvestmentAccount savedAccount = investmentAccountRepository.save(account);
        return InvestmentAccountCreateRes.from(savedAccount);
    }

    @Transactional
    public InvestmentAccountUpdateRes updateAccount(
            Long userId,
            Long accountId,
            InvestmentAccountUpdateReq request
    ) {
        InvestmentAccount account = getActiveAccountForUpdate(userId, accountId);
        account.verifyPassword(passwordEncoder, request.pastPassword());

        /** dto 검증과 별개로 서비스 레이어에서 입력값 검증 */
        if (request.nickname() == null && request.newPassword() == null) {
            throw new BusinessException(InvestmentErrorCode.INVESTMENT_ACCOUNT_UPDATE_VALUE_REQUIRED);
        }
        if (request.nickname() != null) {
            account.updateNickname(request.nickname());
        }
        if (request.newPassword() != null) {
            account.changePassword(passwordEncoder.encode(request.newPassword()));
        }

        return InvestmentAccountUpdateRes.from(account);
    }

    private User getVerifiedUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        if (!Boolean.TRUE.equals(user.getIdentityVerified())) {
            throw new BusinessException(InvestmentErrorCode.IDENTITY_VERIFICATION_REQUIRED);
        }

        return user;
    }

    private String generateUniqueAccountNumber() {
        for (int i = 0; i < MAX_ACCOUNT_NUMBER_GENERATION_RETRY; i++) {
            String accountNumber = InvestmentAccountNumberGenerator.generate();

            if (!investmentAccountRepository.existsByAccountNumber(accountNumber)) {
                return accountNumber;
            }
        }

        throw new BusinessException(InvestmentErrorCode.INVESTMENT_ACCOUNT_NUMBER_GENERATION_FAILED);
    }

    private InvestmentAccount getActiveAccountForUpdate(Long userId, Long accountId) {
        InvestmentAccount account = investmentAccountRepository.findByIdAndUserIdForUpdate(accountId, userId)
                .orElseThrow(() -> new BusinessException(InvestmentErrorCode.INVESTMENT_ACCOUNT_NOT_FOUND));

        if (!account.isActive()) {
            throw new BusinessException(InvestmentErrorCode.INVESTMENT_ACCOUNT_NOT_ACTIVE);
        }

        return account;
    }
}
