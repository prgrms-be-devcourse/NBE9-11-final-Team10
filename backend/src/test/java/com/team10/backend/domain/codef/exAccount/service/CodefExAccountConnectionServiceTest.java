package com.team10.backend.domain.codef.exAccount.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team10.backend.domain.codef.exAccount.client.CodefExAccountClient;
import com.team10.backend.domain.codef.exAccount.crypto.CodefConnectedIdEncryptor;
import com.team10.backend.domain.codef.exAccount.dto.internal.CodefExAccountConnectionPayload;
import com.team10.backend.domain.codef.exAccount.dto.internal.CodefExAccountConnectionRegistrationResult;
import com.team10.backend.domain.codef.exAccount.dto.internal.CodefExAccountConnectionResult;
import com.team10.backend.domain.codef.exAccount.dto.internal.CodefExAccountListRequest;
import com.team10.backend.domain.codef.exAccount.dto.internal.CodefExAccountSnapshot;
import com.team10.backend.domain.codef.exAccount.dto.internal.EncryptedConnectedId;
import com.team10.backend.domain.codef.exAccount.dto.req.CodefExAccountConnectionCreateReq;
import com.team10.backend.domain.codef.exAccount.entity.CodefExAccountConnection;
import com.team10.backend.domain.codef.exAccount.exception.CodefExAccountErrorCode;
import com.team10.backend.domain.codef.exAccount.exception.CodefExAccountRegistrationException;
import com.team10.backend.domain.codef.exAccount.exception.CodefExAccountRegistrationFailure;
import com.team10.backend.domain.codef.exAccount.mapper.CodefExAccountConnectionPayloadMapper;
import com.team10.backend.domain.codef.exAccount.mapper.CodefExAccountSnapshotMapper;
import com.team10.backend.domain.codef.exAccount.repository.CodefExAccountConnectionRepository;
import com.team10.backend.domain.codef.exAccount.type.CodefExAccountConnectionStatus;
import com.team10.backend.domain.exAccount.Type.ExAccountType;
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
class CodefExAccountConnectionServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private CodefExAccountConnectionRepository connectionRepository;
    @Mock
    private CodefExAccountConnectionPayloadMapper connectionPayloadMapper;
    @Mock
    private CodefExAccountClient codefExAccountClient;
    @Mock
    private CodefConnectedIdEncryptor connectedIdEncryptor;
    @Mock
    private CodefExAccountSnapshotMapper snapshotMapper;

    private CodefExAccountConnectionService service;
    private User user;
    private CodefExAccountConnectionCreateReq createRequest;
    private CodefExAccountConnectionPayload payload;

    @BeforeEach
    void setUp() {
        service = new CodefExAccountConnectionService(
                userRepository,
                connectionRepository,
                connectionPayloadMapper,
                codefExAccountClient,
                connectedIdEncryptor,
                snapshotMapper
        );
        user = User.create(
                "owner@example.com", "password", "사용자", "01012345678",
                LocalDate.of(1995, 1, 1)
        );
        createRequest = new CodefExAccountConnectionCreateReq(
                "0004", "BK", "P", "1", "internet-user", "bank-password", "950101"
        );
        payload = new CodefExAccountConnectionPayload(List.of(
                new CodefExAccountConnectionPayload.Account(
                        "KR", "BK", "P", "0004", "1",
                        "internet-user", "rsa-password", "950101"
                )
        ));
    }

    @Test
    void registersAndStoresOnlyEncryptedConnectedId() {
        EncryptedConnectedId encrypted = new EncryptedConnectedId("ciphertext", "iv", "v1");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(connectionPayloadMapper.toPayload(createRequest)).thenReturn(payload);
        when(codefExAccountClient.createConnection(payload))
                .thenReturn(new CodefExAccountConnectionResult("plain-connected-id"));
        when(connectedIdEncryptor.encrypt("plain-connected-id")).thenReturn(encrypted);
        when(connectionRepository.findByUserIdAndOrganization(1L, "0004"))
                .thenReturn(Optional.empty());
        when(connectionRepository.save(any(CodefExAccountConnection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CodefExAccountConnectionRegistrationResult result = service.register(1L, createRequest);

        ArgumentCaptor<CodefExAccountConnection> connectionCaptor =
                ArgumentCaptor.forClass(CodefExAccountConnection.class);
        verify(connectionRepository).save(connectionCaptor.capture());
        CodefExAccountConnection saved = connectionCaptor.getValue();
        assertThat(saved.getConnectedIdCiphertext()).isEqualTo("ciphertext");
        assertThat(saved.getConnectedIdIv()).isEqualTo("iv");
        assertThat(saved.getEncryptionKeyVersion()).isEqualTo("v1");
        assertThat(saved.encryptedConnectedId().toString()).doesNotContain("plain-connected-id");
        assertThat(result.organization()).isEqualTo("0004");
        assertThat(result.status()).isEqualTo(CodefExAccountConnectionStatus.ACTIVE);
    }

    @Test
    void updatesExistingConnectionWhenRelinking() {
        CodefExAccountConnection existing = CodefExAccountConnection.create(
                user,
                "0004",
                new EncryptedConnectedId("old-ciphertext", "old-iv", "v0")
        );
        existing.markSynced(java.time.LocalDateTime.now());
        EncryptedConnectedId replacement = new EncryptedConnectedId("new-ciphertext", "new-iv", "v1");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(connectionPayloadMapper.toPayload(createRequest)).thenReturn(payload);
        when(codefExAccountClient.createConnection(payload))
                .thenReturn(new CodefExAccountConnectionResult("new-connected-id"));
        when(connectedIdEncryptor.encrypt("new-connected-id")).thenReturn(replacement);
        when(connectionRepository.findByUserIdAndOrganization(1L, "0004"))
                .thenReturn(Optional.of(existing));
        when(connectionRepository.save(existing)).thenReturn(existing);

        service.register(1L, createRequest);

        assertThat(existing.getConnectedIdCiphertext()).isEqualTo("new-ciphertext");
        assertThat(existing.getConnectedIdIv()).isEqualTo("new-iv");
        assertThat(existing.getEncryptionKeyVersion()).isEqualTo("v1");
        assertThat(existing.getStatus()).isEqualTo(CodefExAccountConnectionStatus.ACTIVE);
        assertThat(existing.getLastSyncedAt()).isNull();
    }

    @Test
    void doesNotStoreConnectionWhenCodefRegistrationFails() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(connectionPayloadMapper.toPayload(createRequest)).thenReturn(payload);
        when(codefExAccountClient.createConnection(payload)).thenThrow(
                new CodefExAccountRegistrationException(
                        CodefExAccountRegistrationFailure.CREDENTIAL_INVALID,
                        "은행 인증정보가 올바르지 않습니다."
                )
        );

        assertThatThrownBy(() -> service.register(1L, createRequest))
                .isInstanceOf(CodefExAccountRegistrationException.class);
        verify(connectedIdEncryptor, never()).encrypt(any());
        verify(connectionRepository, never()).save(any());
    }

    @Test
    void decryptsOwnedConnectionBeforeFetchingAccountSnapshots() throws Exception {
        CodefExAccountConnection connection = CodefExAccountConnection.create(
                user,
                "0004",
                new EncryptedConnectedId("ciphertext", "iv", "v1")
        );
        JsonNode data = new ObjectMapper().readTree("{}");
        CodefExAccountSnapshot snapshot = snapshot();
        when(connectionRepository.findByUserIdAndOrganization(1L, "0004"))
                .thenReturn(Optional.of(connection));
        when(connectedIdEncryptor.decrypt(connection.encryptedConnectedId()))
                .thenReturn("decrypted-connected-id");
        when(codefExAccountClient.getAccountList(any(CodefExAccountListRequest.class)))
                .thenReturn(data);
        when(snapshotMapper.toSnapshots("0004", data)).thenReturn(List.of(snapshot));

        List<CodefExAccountSnapshot> result = service.getAccountSnapshots(1L, "0004");

        ArgumentCaptor<CodefExAccountListRequest> requestCaptor =
                ArgumentCaptor.forClass(CodefExAccountListRequest.class);
        verify(codefExAccountClient).getAccountList(requestCaptor.capture());
        assertThat(requestCaptor.getValue().connectedId()).isEqualTo("decrypted-connected-id");
        assertThat(requestCaptor.getValue().toString()).doesNotContain("decrypted-connected-id");
        assertThat(result).containsExactly(snapshot);
        assertThat(connection.getLastSyncedAt()).isNotNull();
    }

    @Test
    void blocksAnotherUsersConnectionLookup() {
        when(connectionRepository.findByUserIdAndOrganization(2L, "0004"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAccountSnapshots(2L, "0004"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(CodefExAccountErrorCode.CODEF_CONNECTION_NOT_FOUND));
        verify(connectedIdEncryptor, never()).decrypt(any());
        verify(codefExAccountClient, never()).getAccountList(any());
    }

    @Test
    void blocksInactiveConnectionBeforeDecryption() {
        CodefExAccountConnection connection = CodefExAccountConnection.create(
                user,
                "0004",
                new EncryptedConnectedId("ciphertext", "iv", "v1")
        );
        connection.requireReauthentication();
        when(connectionRepository.findByUserIdAndOrganization(1L, "0004"))
                .thenReturn(Optional.of(connection));

        assertThatThrownBy(() -> service.getAccountSnapshots(1L, "0004"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(CodefExAccountErrorCode.CODEF_CONNECTION_INACTIVE));
        verify(connectedIdEncryptor, never()).decrypt(any());
    }

    private CodefExAccountSnapshot snapshot() {
        return new CodefExAccountSnapshot(
                "0004", "1234567890", "입출금통장", "생활비",
                ExAccountType.DEMAND, BigDecimal.valueOf(1000), BigDecimal.valueOf(900),
                LocalDate.of(2024, 1, 1), null, LocalDate.of(2026, 6, 22)
        );
    }
}
