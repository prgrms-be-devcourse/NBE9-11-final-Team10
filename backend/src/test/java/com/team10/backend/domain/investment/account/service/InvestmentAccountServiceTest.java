package com.team10.backend.domain.investment.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.team10.backend.domain.investment.account.dto.req.InvestmentAccountCreateReq;
import com.team10.backend.domain.investment.account.dto.res.InvestmentAccountCreateRes;
import com.team10.backend.domain.investment.account.dto.res.InvestmentAccountOpenVerificationRes;
import com.team10.backend.domain.investment.account.entity.InvestmentAccount;
import com.team10.backend.domain.investment.account.repository.InvestmentAccountRepository;
import com.team10.backend.domain.investment.account.type.InvestmentAccountStatus;
import com.team10.backend.domain.investment.exception.InvestmentErrorCode;
import com.team10.backend.domain.investment.type.CurrencyCode;
import com.team10.backend.domain.user.entity.User;
import com.team10.backend.domain.user.exception.UserErrorCode;
import com.team10.backend.domain.user.repository.UserRepository;
import com.team10.backend.global.exception.BusinessException;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class InvestmentAccountServiceTest {

    @Mock
    private InvestmentAccountRepository investmentAccountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private InvestmentAccountOpenVerificationKeyService verificationKeyService;

    @InjectMocks
    private InvestmentAccountService investmentAccountService;

    private User verifiedUser;
    private User unverifiedUser;

    @BeforeEach
    void setUp() {
        verifiedUser = createUser(1L, true);
        unverifiedUser = createUser(2L, false);
    }

    @Test
    @DisplayName("본인인증 완료 사용자는 투자 계좌 개설 인증키를 발급받을 수 있다")
    void issueOpenVerificationKey() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(verifiedUser));
        when(verificationKeyService.generateAndStore(1L)).thenReturn("verification-key");
        when(verificationKeyService.ttlSeconds()).thenReturn(600L);

        InvestmentAccountOpenVerificationRes response =
                investmentAccountService.issueOpenVerificationKey(1L);

        assertThat(response.verificationKey()).isEqualTo("verification-key");
        assertThat(response.expiresInSeconds()).isEqualTo(600L);
    }

    @Test
    @DisplayName("본인인증 미완료 사용자는 투자 계좌 개설 인증키를 발급받을 수 없다")
    void issueOpenVerificationKeyWithoutIdentityVerification() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(unverifiedUser));

        assertThatThrownBy(() -> investmentAccountService.issueOpenVerificationKey(2L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(InvestmentErrorCode.IDENTITY_VERIFICATION_REQUIRED);
    }

    @Test
    @DisplayName("본인인증과 개설 인증키 검증을 통과하면 투자 계좌를 개설한다")
    void createAccount() {
        InvestmentAccountCreateReq request =
                new InvestmentAccountCreateReq("모의투자 계좌", "123456", "verification-key", CurrencyCode.KRW);

        when(userRepository.findById(1L)).thenReturn(Optional.of(verifiedUser));
        when(investmentAccountRepository.existsByAccountNumber(any(String.class))).thenReturn(false);
        when(verificationKeyService.verifyAndDelete(1L, "verification-key")).thenReturn(true);
        when(passwordEncoder.encode("123456")).thenReturn("encoded-password");
        when(investmentAccountRepository.save(any(InvestmentAccount.class))).thenAnswer(invocation -> {
            InvestmentAccount account = invocation.getArgument(0);
            ReflectionTestUtils.setField(account, "id", 1L);
            return account;
        });

        InvestmentAccountCreateRes response = investmentAccountService.createAccount(1L, request);
        
        assertThat(response.accountNumber()).matches("\\d{10}-\\d{2}");
        assertThat(response.nickname()).isEqualTo("모의투자 계좌");
        assertThat(response.cashBalance()).isZero();
        assertThat(response.currencyCode()).isEqualTo(CurrencyCode.KRW);
        assertThat(response.status()).isEqualTo(InvestmentAccountStatus.ACTIVE);

        ArgumentCaptor<InvestmentAccount> captor = ArgumentCaptor.forClass(InvestmentAccount.class);
        verify(investmentAccountRepository).save(captor.capture());
        assertThat(captor.getValue().getAccountPasswordHash()).isEqualTo("encoded-password");
    }

    @Test
    @DisplayName("존재하지 않는 사용자는 투자 계좌를 개설할 수 없다")
    void createAccountWithNotFoundUser() {
        InvestmentAccountCreateReq request =
                new InvestmentAccountCreateReq("모의투자 계좌", "123456", "verification-key", CurrencyCode.KRW);

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> investmentAccountService.createAccount(999L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("본인인증 미완료 사용자는 투자 계좌를 개설할 수 없다")
    void createAccountWithoutIdentityVerification() {
        InvestmentAccountCreateReq request =
                new InvestmentAccountCreateReq("모의투자 계좌", "123456", "verification-key", CurrencyCode.KRW);

        when(userRepository.findById(2L)).thenReturn(Optional.of(unverifiedUser));

        assertThatThrownBy(() -> investmentAccountService.createAccount(2L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(InvestmentErrorCode.IDENTITY_VERIFICATION_REQUIRED);
    }

    @Test
    @DisplayName("개설 인증키가 유효하지 않으면 투자 계좌를 개설할 수 없다")
    void createAccountWithInvalidVerificationKey() {
        InvestmentAccountCreateReq request =
                new InvestmentAccountCreateReq("모의투자 계좌", "123456", "invalid-key", CurrencyCode.KRW);

        when(userRepository.findById(1L)).thenReturn(Optional.of(verifiedUser));
        when(investmentAccountRepository.existsByAccountNumber(any(String.class))).thenReturn(false);
        when(verificationKeyService.verifyAndDelete(1L, "invalid-key")).thenReturn(false);

        assertThatThrownBy(() -> investmentAccountService.createAccount(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(InvestmentErrorCode.INVESTMENT_ACCOUNT_OPEN_VERIFICATION_KEY_INVALID);

        verify(investmentAccountRepository, never()).save(any(InvestmentAccount.class));
    }

    @Test
    @DisplayName("계좌번호 생성 최대 시도 횟수를 초과하면 투자 계좌 개설에 실패한다")
    void createAccountWithAccountNumberGenerationFailed() {
        InvestmentAccountCreateReq request =
                new InvestmentAccountCreateReq("모의투자 계좌", "123456", "verification-key", CurrencyCode.KRW);

        when(userRepository.findById(1L)).thenReturn(Optional.of(verifiedUser));
        when(investmentAccountRepository.existsByAccountNumber(any(String.class))).thenReturn(true);

        assertThatThrownBy(() -> investmentAccountService.createAccount(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(InvestmentErrorCode.INVESTMENT_ACCOUNT_NUMBER_GENERATION_FAILED);

        verify(verificationKeyService, never()).verifyAndDelete(any(Long.class), any(String.class));
    }

    private User createUser(Long id, boolean identityVerified) {
        User user = User.create(
                "user" + id + "@example.com",
                "encoded-password",
                "사용자" + id,
                "01012345678",
                LocalDate.of(1995, 1, 1)
        );
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "identityVerified", identityVerified);
        return user;
    }
}
