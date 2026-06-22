package com.team10.backend.domain.exAccount.service;

import com.team10.backend.domain.codef.exAccount.dto.internal.CodefExAccountSnapshot;
import com.team10.backend.domain.codef.exAccount.dto.req.CodefExAccountConnectionCreateReq;
import com.team10.backend.domain.codef.exAccount.service.CodefExAccountGateway;
import com.team10.backend.domain.exAccount.type.ExAccountConnectionStatus;
import com.team10.backend.domain.exAccount.type.ExAccountType;
import com.team10.backend.domain.exAccount.dto.res.ExAccountCandidateRes;
import com.team10.backend.domain.exAccount.dto.res.ExAccountConnectionRes;
import com.team10.backend.domain.exAccount.entity.ExAccountConnection;
import com.team10.backend.domain.exAccount.entity.value.EncryptedConnectedId;
import com.team10.backend.domain.exAccount.exception.ExAccountConnectionErrorCode;
import com.team10.backend.domain.exAccount.repository.ExAccountConnectionRepository;
import com.team10.backend.domain.user.entity.User;
import com.team10.backend.domain.user.repository.UserRepository;
import com.team10.backend.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExAccountConnectionServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ExAccountConnectionRepository connectionRepository;
    @Mock
    private CodefExAccountGateway codefExAccountGateway;
    @Mock
    private ExAccountSyncService exAccountSyncService;

    private ExAccountConnectionService service;
    private User user;
    private CodefExAccountConnectionCreateReq createRequest;

    @BeforeEach
    void setUp() {
        service = new ExAccountConnectionService(
                userRepository,
                connectionRepository,
                codefExAccountGateway,
                exAccountSyncService
        );
        user = User.create(
                "owner@example.com", "password", "사용자", "01012345678",
                LocalDate.of(1995, 1, 1)
        );
        createRequest = new CodefExAccountConnectionCreateReq(
                "0004", "BK", "P", "1", "internet-user", "bank-password", "950101"
        );
    }

    @Test
    void registersAndStoresOnlyEncryptedConnectedId() {
        EncryptedConnectedId encrypted = new EncryptedConnectedId("ciphertext", "iv", "v1");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(codefExAccountGateway.register(createRequest)).thenReturn(encrypted);
        when(connectionRepository.findByUserIdAndOrganization(1L, "0004"))
                .thenReturn(Optional.empty());
        when(connectionRepository.save(any(ExAccountConnection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ExAccountConnectionRes result = service.register(1L, createRequest);

        ArgumentCaptor<ExAccountConnection> captor =
                ArgumentCaptor.forClass(ExAccountConnection.class);
        verify(connectionRepository).save(captor.capture());
        ExAccountConnection saved = captor.getValue();
        assertThat(saved.getConnectedIdCiphertext()).isEqualTo("ciphertext");
        assertThat(saved.getConnectedIdIv()).isEqualTo("iv");
        assertThat(saved.getEncryptionKeyVersion()).isEqualTo("v1");
        assertThat(result.status()).isEqualTo(ExAccountConnectionStatus.ACTIVE);
    }

    @Test
    void updatesExistingConnectionWhenRelinking() {
        ExAccountConnection existing = connection("old-ciphertext");
        existing.markSynced(java.time.LocalDateTime.now());
        EncryptedConnectedId replacement = new EncryptedConnectedId(
                "new-ciphertext", "new-iv", "v1"
        );
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(codefExAccountGateway.register(createRequest)).thenReturn(replacement);
        when(connectionRepository.findByUserIdAndOrganization(1L, "0004"))
                .thenReturn(Optional.of(existing));
        when(connectionRepository.save(existing)).thenReturn(existing);

        service.register(1L, createRequest);

        assertThat(existing.getConnectedIdCiphertext()).isEqualTo("new-ciphertext");
        assertThat(existing.getStatus()).isEqualTo(ExAccountConnectionStatus.ACTIVE);
        assertThat(existing.getLastSyncedAt()).isNull();
    }

    @Test
    void getsProviderAccountsAndReturnsMaskedCandidates() {
        ExAccountConnection connection = connection("ciphertext");
        CodefExAccountSnapshot snapshot = snapshot();
        ExAccountCandidateRes candidate = candidate();
        when(connectionRepository.findByUserIdAndOrganization(1L, "0004"))
                .thenReturn(Optional.of(connection));
        when(codefExAccountGateway.getAccountSnapshots(
                "0004", connection.encryptedConnectedId()
        )).thenReturn(List.of(snapshot));
        when(exAccountSyncService.getLinkCandidates(any(), any()))
                .thenReturn(List.of(candidate));

        List<ExAccountCandidateRes> result = service.getLinkCandidates(1L, "0004");

        assertThat(result).containsExactly(candidate);
        assertThat(result.getFirst().accountNoMasked()).doesNotContain("1234567890");
        assertThat(connection.getLastSyncedAt()).isNotNull();
        verify(exAccountSyncService).getLinkCandidates(any(), any());
    }

    @Test
    void returnsEmptyCandidatesWhenProviderHasNoAccounts() {
        ExAccountConnection connection = connection("ciphertext");
        when(connectionRepository.findByUserIdAndOrganization(1L, "0004"))
                .thenReturn(Optional.of(connection));
        when(codefExAccountGateway.getAccountSnapshots(
                "0004", connection.encryptedConnectedId()
        )).thenReturn(List.of());

        assertThat(service.getLinkCandidates(1L, "0004")).isEmpty();
        assertThat(connection.getLastSyncedAt()).isNotNull();
        verify(exAccountSyncService, never()).getLinkCandidates(any(), any());
    }

    @Test
    void blocksAnotherUsersConnectionLookup() {
        when(connectionRepository.findByUserIdAndOrganization(2L, "0004"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLinkCandidates(2L, "0004"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(
                                ExAccountConnectionErrorCode.EX_ACCOUNT_CONNECTION_NOT_FOUND));
        verify(codefExAccountGateway, never()).getAccountSnapshots(any(), any());
    }

    @Test
    void blocksInactiveConnectionBeforeCallingProvider() {
        ExAccountConnection connection = connection("ciphertext");
        connection.requireReauthentication();
        when(connectionRepository.findByUserIdAndOrganization(1L, "0004"))
                .thenReturn(Optional.of(connection));

        assertThatThrownBy(() -> service.getLinkCandidates(1L, "0004"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(
                                ExAccountConnectionErrorCode.EX_ACCOUNT_CONNECTION_INACTIVE));
        verify(codefExAccountGateway, never()).getAccountSnapshots(any(), any());
    }

    private ExAccountConnection connection(String ciphertext) {
        return ExAccountConnection.create(
                user,
                "0004",
                new EncryptedConnectedId(ciphertext, "iv", "v1")
        );
    }

    private CodefExAccountSnapshot snapshot() {
        return new CodefExAccountSnapshot(
                "0004", "1234567890", "입출금통장", "생활비",
                ExAccountType.DEMAND, BigDecimal.valueOf(1000), BigDecimal.valueOf(900),
                LocalDate.of(2024, 1, 1), null, LocalDate.of(2026, 6, 22)
        );
    }

    private ExAccountCandidateRes candidate() {
        return new ExAccountCandidateRes(
                "0004", "123***7890", "입출금통장", "생활비",
                ExAccountType.DEMAND, BigDecimal.valueOf(1000), BigDecimal.valueOf(900),
                LocalDate.of(2024, 1, 1), null, LocalDate.of(2026, 6, 22), false
        );
    }
}
