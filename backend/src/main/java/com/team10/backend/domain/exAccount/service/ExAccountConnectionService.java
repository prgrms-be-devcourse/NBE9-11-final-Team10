package com.team10.backend.domain.exAccount.service;

import com.team10.backend.domain.codef.exAccount.dto.internal.CodefExAccountSnapshot;
import com.team10.backend.domain.codef.exAccount.dto.req.CodefExAccountConnectionCreateReq;
import com.team10.backend.domain.codef.exAccount.service.CodefExAccountGateway;
import com.team10.backend.domain.codef.exAccount.store.CodefExAccountCandidateStore;
import com.team10.backend.domain.exAccount.dto.res.ExAccountCandidateListRes;
import com.team10.backend.domain.exAccount.dto.res.ExAccountCandidateRes;
import com.team10.backend.domain.exAccount.dto.res.ExAccountConnectionRes;
import com.team10.backend.domain.exAccount.entity.ExAccountConnection;
import com.team10.backend.domain.exAccount.entity.value.EncryptedConnectedId;
import com.team10.backend.domain.exAccount.exception.ExAccountConnectionErrorCode;
import com.team10.backend.domain.exAccount.repository.ExAccountConnectionRepository;
import com.team10.backend.domain.exAccount.repository.ExAccountRepository;
import com.team10.backend.domain.user.entity.User;
import com.team10.backend.domain.user.exception.UserErrorCode;
import com.team10.backend.domain.user.repository.UserRepository;
import com.team10.backend.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExAccountConnectionService {

    private final UserRepository userRepository;
    private final ExAccountConnectionRepository connectionRepository;
    private final CodefExAccountGateway codefExAccountGateway;
    private final ExAccountSyncService exAccountSyncService;
    private final ExAccountRepository exAccountRepository;
    private final CodefExAccountCandidateStore candidateStore;

    @Transactional
    public ExAccountConnectionRes register(
            Long userId,
            CodefExAccountConnectionCreateReq request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        EncryptedConnectedId encryptedConnectedId = codefExAccountGateway.register(request);

        ExAccountConnection connection = connectionRepository
                .findByUserIdAndOrganization(userId, request.organization())
                .map(existing -> updateConnection(existing, encryptedConnectedId))
                .orElseGet(() -> ExAccountConnection.create(
                        user,
                        request.organization(),
                        encryptedConnectedId
                ));
        ExAccountConnection saved = connectionRepository.save(connection);
        return new ExAccountConnectionRes(saved.getOrganization(), saved.getStatus());
    }

    @Transactional
    public ExAccountCandidateListRes getLinkCandidates(Long userId, String organization) {
        ExAccountConnection connection = getActiveConnection(userId, organization);
        List<CodefExAccountSnapshot> snapshots = codefExAccountGateway
                .getAccountSnapshots(organization, connection.encryptedConnectedId());

        connection.markSynced(LocalDateTime.now(ZoneOffset.UTC));
        if (snapshots.isEmpty()) {
            return new ExAccountCandidateListRes("", 300, List.of());
        }

        // Redis 저장 및 토큰 발급
        String candidateToken = candidateStore.save(userId, snapshots);

        // 후보 응답 리스트 빌드
        List<ExAccountCandidateRes> accounts = new ArrayList<>();
        for (int i = 0; i < snapshots.size(); i++) {
            CodefExAccountSnapshot snapshot = snapshots.get(i);
            String hash = exAccountSyncService.getAccountNumberHash(snapshot.accountNumber());
            String masked = exAccountSyncService.getMaskedAccountNumber(snapshot.accountNumber());

            boolean linked = exAccountRepository
                    .findByUserIdAndOrganizationAndAccountNumberHash(userId, organization, hash)
                    .isPresent();

            accounts.add(ExAccountCandidateRes.from(i, snapshot, masked, linked));
        }

        return new ExAccountCandidateListRes(candidateToken, 300, accounts);
    }

    private ExAccountConnection getActiveConnection(Long userId, String organization) {
        ExAccountConnection connection = connectionRepository
                .findByUserIdAndOrganization(userId, organization)
                .orElseThrow(() -> new BusinessException(
                        ExAccountConnectionErrorCode.EX_ACCOUNT_CONNECTION_NOT_FOUND));
        if (!connection.isActive()) {
            throw new BusinessException(
                    ExAccountConnectionErrorCode.EX_ACCOUNT_CONNECTION_INACTIVE);
        }
        return connection;
    }

    private ExAccountConnection updateConnection(
            ExAccountConnection existing,
            EncryptedConnectedId encryptedConnectedId
    ) {
        existing.replaceConnectedId(encryptedConnectedId);
        return existing;
    }
}
