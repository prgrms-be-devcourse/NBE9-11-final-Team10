package com.team10.backend.domain.codef.exAccount.service;

import com.fasterxml.jackson.databind.JsonNode;
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
import com.team10.backend.domain.codef.exAccount.mapper.CodefExAccountConnectionPayloadMapper;
import com.team10.backend.domain.codef.exAccount.mapper.CodefExAccountSnapshotMapper;
import com.team10.backend.domain.codef.exAccount.repository.CodefExAccountConnectionRepository;
import com.team10.backend.domain.user.entity.User;
import com.team10.backend.domain.user.exception.UserErrorCode;
import com.team10.backend.domain.user.repository.UserRepository;
import com.team10.backend.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CodefExAccountConnectionService {

    private final UserRepository userRepository;
    private final CodefExAccountConnectionRepository connectionRepository;
    private final CodefExAccountConnectionPayloadMapper connectionPayloadMapper;
    private final CodefExAccountClient codefExAccountClient;
    private final CodefConnectedIdEncryptor connectedIdEncryptor;
    private final CodefExAccountSnapshotMapper snapshotMapper;
    private final Clock clock = Clock.systemUTC();

    @Transactional
    public CodefExAccountConnectionRegistrationResult register(
            Long userId,
            CodefExAccountConnectionCreateReq request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        CodefExAccountConnectionPayload payload = connectionPayloadMapper.toPayload(request);
        CodefExAccountConnectionResult result = codefExAccountClient.createConnection(payload);
        EncryptedConnectedId encryptedConnectedId = connectedIdEncryptor.encrypt(result.connectedId());

        CodefExAccountConnection connection = connectionRepository
                .findByUserIdAndOrganization(userId, request.organization())
                .map(existing -> updateConnection(existing, encryptedConnectedId))
                .orElseGet(() -> CodefExAccountConnection.create(
                        user,
                        request.organization(),
                        encryptedConnectedId
                ));
        CodefExAccountConnection saved = connectionRepository.save(connection);
        return new CodefExAccountConnectionRegistrationResult(
                saved.getOrganization(),
                saved.getStatus()
        );
    }

    @Transactional
    public List<CodefExAccountSnapshot> getAccountSnapshots(Long userId, String organization) {
        CodefExAccountConnection connection = connectionRepository
                .findByUserIdAndOrganization(userId, organization)
                .orElseThrow(() -> new BusinessException(
                        CodefExAccountErrorCode.CODEF_CONNECTION_NOT_FOUND));
        if (!connection.isActive()) {
            throw new BusinessException(CodefExAccountErrorCode.CODEF_CONNECTION_INACTIVE);
        }

        String connectedId = connectedIdEncryptor.decrypt(connection.encryptedConnectedId());
        CodefExAccountListRequest request = CodefExAccountListRequest.of(
                organization,
                connectedId,
                ""
        );
        JsonNode data = codefExAccountClient.getAccountList(request);
        List<CodefExAccountSnapshot> snapshots = snapshotMapper.toSnapshots(organization, data);
        connection.markSynced(LocalDateTime.now(clock));
        return snapshots;
    }

    private CodefExAccountConnection updateConnection(
            CodefExAccountConnection existing,
            EncryptedConnectedId encryptedConnectedId
    ) {
        existing.replaceConnectedId(encryptedConnectedId);
        return existing;
    }
}
