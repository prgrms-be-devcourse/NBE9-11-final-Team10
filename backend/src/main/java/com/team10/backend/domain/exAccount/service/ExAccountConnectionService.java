package com.team10.backend.domain.exAccount.service;

import com.team10.backend.domain.codef.exAccount.dto.internal.CodefExAccountSnapshot;
import com.team10.backend.domain.codef.exAccount.dto.req.CodefExAccountConnectionCreateReq;
import com.team10.backend.domain.codef.exAccount.service.CodefExAccountGateway;
import com.team10.backend.domain.exAccount.dto.req.ExAccountLinkReq;
import com.team10.backend.domain.exAccount.dto.res.ExAccountCandidateRes;
import com.team10.backend.domain.exAccount.dto.res.ExAccountConnectionRes;
import com.team10.backend.domain.exAccount.entity.ExAccountConnection;
import com.team10.backend.domain.exAccount.entity.value.EncryptedConnectedId;
import com.team10.backend.domain.exAccount.exception.ExAccountConnectionErrorCode;
import com.team10.backend.domain.exAccount.repository.ExAccountConnectionRepository;
import com.team10.backend.domain.user.entity.User;
import com.team10.backend.domain.user.exception.UserErrorCode;
import com.team10.backend.domain.user.repository.UserRepository;
import com.team10.backend.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExAccountConnectionService {

    private final UserRepository userRepository;
    private final ExAccountConnectionRepository connectionRepository;
    private final CodefExAccountGateway codefExAccountGateway;
    private final ExAccountSyncService exAccountSyncService;

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
    public List<ExAccountCandidateRes> getLinkCandidates(Long userId, String organization) {
        ExAccountConnection connection = getActiveConnection(userId, organization);
        List<ExAccountLinkReq> accounts = codefExAccountGateway
                .getAccountSnapshots(organization, connection.encryptedConnectedId())
                .stream()
                .map(this::toLinkRequest)
                .toList();

        connection.markSynced(LocalDateTime.now(ZoneOffset.UTC));
        if (accounts.isEmpty()) {
            return List.of();
        }
        return exAccountSyncService.getLinkCandidates(userId, accounts);
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

    private ExAccountLinkReq toLinkRequest(CodefExAccountSnapshot snapshot) {
        return new ExAccountLinkReq(
                snapshot.organization(),
                snapshot.accountNumber(),
                snapshot.accountName(),
                snapshot.accountAlias(),
                snapshot.assetType(),
                snapshot.balance(),
                snapshot.withdrawableAmount(),
                snapshot.openedAt(),
                snapshot.maturityAt(),
                snapshot.lastTransactionAt()
        );
    }
}
