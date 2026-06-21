package com.team10.backend.domain.exAccount.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.team10.backend.domain.exAccount.Type.ExAccountStatus;
import com.team10.backend.domain.exAccount.Type.ExAccountType;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ExAccountSyncServiceTest {

    private static final String ACCOUNT_NUMBER_HASH = "a".repeat(64);

    @Mock
    private ExAccountRepository exAccountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HmacSha256Hasher hmacSha256Hasher;

    @InjectMocks
    private ExAccountSyncService exAccountSyncService;

    private User user;

    @BeforeEach
    void setUp() {
        user = createUser(1L);
    }

    @Test
    @DisplayName("외부 계좌 후보 목록이 비어 있으면 공통 입력 오류로 실패한다")
    void getLinkCandidatesWithEmptyRequests() {
        assertThatThrownBy(() -> exAccountSyncService.getLinkCandidates(1L, List.of()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(GlobalErrorCode.INVALID_INPUT_VALUE);

        verify(exAccountRepository, never())
                .findByUserIdAndOrganizationAndAccountNumberHash(any(), any(), any());
    }

    @Test
    @DisplayName("외부 계좌 후보 조회는 DB에 저장하지 않고 연동 여부만 반환한다")
    void getLinkCandidates() {
        ExAccountLinkReq request = createLinkReq("국민은행", "123-456 7890-1234", "KB Star 입출금통장");

        when(hmacSha256Hasher.hash("12345678901234")).thenReturn(ACCOUNT_NUMBER_HASH);
        when(exAccountRepository.findByUserIdAndOrganizationAndAccountNumberHash(
                1L,
                "국민은행",
                ACCOUNT_NUMBER_HASH
        )).thenReturn(Optional.empty());

        List<ExAccountCandidateRes> responses = exAccountSyncService.getLinkCandidates(1L, List.of(request));

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).organization()).isEqualTo("국민은행");
        assertThat(responses.get(0).accountNoMasked()).isEqualTo("123456****1234");
        assertThat(responses.get(0).linked()).isFalse();

        verify(exAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("10자리 계좌번호도 최소 3자리를 마스킹한다")
    void getLinkCandidatesMasksTenDigitAccountNumber() {
        ExAccountLinkReq request = createLinkReq("국민은행", "1234567890", "KB Star 입출금통장");

        when(hmacSha256Hasher.hash("1234567890")).thenReturn(ACCOUNT_NUMBER_HASH);
        when(exAccountRepository.findByUserIdAndOrganizationAndAccountNumberHash(
                1L,
                "국민은행",
                ACCOUNT_NUMBER_HASH
        )).thenReturn(Optional.empty());

        List<ExAccountCandidateRes> responses = exAccountSyncService.getLinkCandidates(1L, List.of(request));

        assertThat(responses.get(0).accountNoMasked()).isEqualTo("123***7890");
    }

    @Test
    @DisplayName("짧은 계좌번호도 가능한 범위에서 최소 3자리를 마스킹한다")
    void getLinkCandidatesMasksShortAccountNumber() {
        ExAccountLinkReq request = createLinkReq("국민은행", "123456", "KB Star 입출금통장");

        when(hmacSha256Hasher.hash("123456")).thenReturn(ACCOUNT_NUMBER_HASH);
        when(exAccountRepository.findByUserIdAndOrganizationAndAccountNumberHash(
                1L,
                "국민은행",
                ACCOUNT_NUMBER_HASH
        )).thenReturn(Optional.empty());

        List<ExAccountCandidateRes> responses = exAccountSyncService.getLinkCandidates(1L, List.of(request));

        assertThat(responses.get(0).accountNoMasked()).isEqualTo("***456");
    }

    @Test
    @DisplayName("이미 연동된 외부 계좌 후보는 linked true로 반환한다")
    void getLinkCandidatesWithLinkedAccount() {
        ExAccountLinkReq request = createLinkReq("국민은행", "12345678901234", "KB Star 입출금통장");
        ExAccount account = createExAccount(10L, user, request);

        when(hmacSha256Hasher.hash("12345678901234")).thenReturn(ACCOUNT_NUMBER_HASH);
        when(exAccountRepository.findByUserIdAndOrganizationAndAccountNumberHash(
                1L,
                "국민은행",
                ACCOUNT_NUMBER_HASH
        )).thenReturn(Optional.of(account));

        List<ExAccountCandidateRes> responses = exAccountSyncService.getLinkCandidates(1L, List.of(request));

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).linked()).isTrue();
        verify(exAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("연동 버튼을 누른 외부 계좌가 없으면 신규 저장한다")
    void linkAccountCreate() {
        ExAccountLinkReq request = createLinkReq("국민은행", "12345678901234", "KB Star 입출금통장");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(hmacSha256Hasher.hash("12345678901234")).thenReturn(ACCOUNT_NUMBER_HASH);
        when(exAccountRepository.findByUserIdAndOrganizationAndAccountNumberHash(
                1L,
                "국민은행",
                ACCOUNT_NUMBER_HASH
        )).thenReturn(Optional.empty());
        when(exAccountRepository.save(any(ExAccount.class))).thenAnswer(invocation -> {
            ExAccount account = invocation.getArgument(0);
            ReflectionTestUtils.setField(account, "id", 10L);
            return account;
        });

        ExAccountRes response = exAccountSyncService.linkAccount(1L, request);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.organization()).isEqualTo("국민은행");
        assertThat(response.accountNoMasked()).isEqualTo("123456****1234");
        assertThat(response.accountName()).isEqualTo("KB Star 입출금통장");
        assertThat(response.status()).isEqualTo(ExAccountStatus.ACTIVE);

        ArgumentCaptor<ExAccount> accountCaptor = ArgumentCaptor.forClass(ExAccount.class);
        verify(exAccountRepository).save(accountCaptor.capture());
        assertThat(accountCaptor.getValue().getAccountNumberHash()).isEqualTo(ACCOUNT_NUMBER_HASH);
        assertThat(accountCaptor.getValue().getAccountNumberMasked()).isEqualTo("123456****1234");
    }

    @Test
    @DisplayName("이미 연동된 외부 계좌는 새로 저장하지 않고 스냅샷을 갱신한다")
    void linkAccountUpdate() {
        ExAccountLinkReq originalRequest = createLinkReq("국민은행", "12345678901234", "KB Star 입출금통장");
        ExAccount account = createExAccount(10L, user, originalRequest);
        ExAccountLinkReq updateRequest = createLinkReq("국민은행", "12345678901234", "급여 통장");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(hmacSha256Hasher.hash("12345678901234")).thenReturn(ACCOUNT_NUMBER_HASH);
        when(exAccountRepository.findByUserIdAndOrganizationAndAccountNumberHash(
                1L,
                "국민은행",
                ACCOUNT_NUMBER_HASH
        )).thenReturn(Optional.of(account));

        ExAccountRes response = exAccountSyncService.linkAccount(1L, updateRequest);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.accountName()).isEqualTo("급여 통장");
        verify(exAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 사용자는 외부 계좌를 연동할 수 없다")
    void linkAccountWithNotFoundUser() {
        ExAccountLinkReq request = createLinkReq("국민은행", "12345678901234", "KB Star 입출금통장");

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> exAccountSyncService.linkAccount(999L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);

        verify(exAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("필수값이 누락된 외부 계좌는 연동할 수 없다")
    void linkAccountWithRequiredFieldMissing() {
        ExAccountLinkReq request = createLinkReq("국민은행", "", "KB Star 입출금통장");

        assertThatThrownBy(() -> exAccountSyncService.linkAccount(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(GlobalErrorCode.INVALID_INPUT_VALUE);

        verify(userRepository, never()).findById(any());
        verify(exAccountRepository, never()).save(any());
    }

    private ExAccountLinkReq createLinkReq(String organization, String accountNumber, String accountName) {
        return new ExAccountLinkReq(
                organization,
                accountNumber,
                accountName,
                "생활비 통장",
                ExAccountType.DEMAND,
                BigDecimal.valueOf(1_500_000),
                BigDecimal.valueOf(1_200_000),
                LocalDate.of(2024, 1, 15),
                null,
                LocalDate.of(2026, 6, 18)
        );
    }

    private ExAccount createExAccount(Long id, User user, ExAccountLinkReq request) {
        ExAccount account = request.toEntity(user, ACCOUNT_NUMBER_HASH, "123456****1234");
        ReflectionTestUtils.setField(account, "id", id);
        return account;
    }

    private User createUser(Long id) {
        User user = User.create(
                "user" + id + "@example.com",
                "password",
                "홍길동",
                "01012345678",
                LocalDate.of(1995, 1, 1)
        );
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
