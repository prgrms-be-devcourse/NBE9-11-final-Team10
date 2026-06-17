package com.team10.backend.domain.investment.account.service;

import com.team10.backend.domain.investment.account.dto.req.InvestmentAccountCreateReq;
import com.team10.backend.domain.investment.account.dto.res.InvestmentAccountCreateRes;
import com.team10.backend.domain.investment.account.dto.res.InvestmentAccountOpenVerificationRes;
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

    /**
     * 존재하는 사용자 여부 및 인증 여부를 검증한다
     */
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
}
