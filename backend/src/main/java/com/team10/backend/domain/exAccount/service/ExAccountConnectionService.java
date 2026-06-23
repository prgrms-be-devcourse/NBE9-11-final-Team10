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
import org.springframework.dao.DataIntegrityViolationException;
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

    /**
     * 사용자의 특정 금융기관 계정 인증 정보(connectedId)를 등록하고 DB에 저장(암호화)합니다.
     */
    @Transactional
    public ExAccountConnectionRes register(
            Long userId,
            CodefExAccountConnectionCreateReq request
    ) {
        // 1. 유저 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        // 2. CODEF API에 계정을 등록하고, 발급된 connectedId를 AES-GCM 알고리즘으로 양방향 암호화 처리
        EncryptedConnectedId encryptedConnectedId = codefExAccountGateway.register(request);

        // 3. 기존에 해당 기관 연결 정보가 있었다면 갱신하고, 없었다면 신규 저장 처리 (Upsert)
        ExAccountConnection connection = connectionRepository
                .findByUserIdAndOrganization(userId, request.organization())
                .map(existing -> updateConnection(existing, encryptedConnectedId))
                .orElseGet(() -> ExAccountConnection.create(
                        user,
                        request.organization(),
                        encryptedConnectedId
                ));
        ExAccountConnection saved = saveConnection(userId, request.organization(), connection, encryptedConnectedId);
        return new ExAccountConnectionRes(saved.getOrganization(), saved.getStatus());
    }

    private ExAccountConnection saveConnection(
            Long userId,
            String organization,
            ExAccountConnection connection,
            EncryptedConnectedId encryptedConnectedId
    ) {
        try {
            return connectionRepository.saveAndFlush(connection);
        } catch (DataIntegrityViolationException exception) {
            ExAccountConnection concurrentConnection = connectionRepository
                    .findByUserIdAndOrganization(userId, organization)
                    .orElseThrow(() -> exception);
            return updateConnection(concurrentConnection, encryptedConnectedId);
        }
    }

    /**
     * 금융기관 연결 정보를 복호화해 CODEF API로부터 보유 계좌 후보군 목록을 실시간으로 가져옵니다.
     * 가져온 데이터는 검증 세션을 생성해 Redis에 안전히 캐싱합니다.
     */
    @Transactional
    public ExAccountCandidateListRes getLinkCandidates(Long userId, String organization) {
        // 1. 해당 기관의 연결 정보(ExAccountConnection)가 유효(ACTIVE)한 상태인지 확인
        ExAccountConnection connection = getActiveConnection(userId, organization);

        // 2. 암호화된 connectedId를 복호화하여 CODEF API로부터 실시간 보유 계좌 스냅샷 데이터 획득
        List<CodefExAccountSnapshot> snapshots = codefExAccountGateway
                .getAccountSnapshots(organization, connection.encryptedConnectedId());

        // 3. 커넥션의 최종 동기화 시각 업데이트
        connection.markSynced(LocalDateTime.now(ZoneOffset.UTC));

        // 계좌가 전혀 발견되지 않은 경우, 토큰 없이 빈 결과 반환
        if (snapshots.isEmpty()) {
            return new ExAccountCandidateListRes("", 300, List.of());
        }

        // 4. [중요 보안] 획득한 원본 데이터를 Redis에 임시 캐싱(TTL 5분)하고 일회용 candidateToken(UUID) 획득
        String candidateToken = candidateStore.save(userId, snapshots);

        // 5. 클라이언트 응답용 계좌 후보군 정보 빌드
        List<ExAccountCandidateRes> accounts = new ArrayList<>();
        for (int i = 0; i < snapshots.size(); i++) {
            CodefExAccountSnapshot snapshot = snapshots.get(i);
            
            // 계좌 번호 암호 해싱(Blind Index) 및 마스킹 처리
            String hash = exAccountSyncService.getAccountNumberHash(snapshot.accountNumber());
            String masked = exAccountSyncService.getMaskedAccountNumber(snapshot.accountNumber());

            // 이미 연동(DB 등록)이 완료된 상태의 계좌인지 판별
            boolean linked = exAccountRepository
                    .findByUserIdAndOrganizationAndAccountNumberHash(userId, organization, hash)
                    .isPresent();

            // 인덱스 번호(i)와 연동 여부를 함께 실어 반환
            accounts.add(ExAccountCandidateRes.from(i, snapshot, masked, linked));
        }

        return new ExAccountCandidateListRes(candidateToken, 300, accounts);
    }

    /**
     * 연결 정보가 유효하고 가용한 상태(ACTIVE)인지 판별하는 헬퍼 메서드입니다.
     */
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

    /**
     * 기존 연결 정보의 암호화된 connectedId를 교체 갱신합니다.
     */
    private ExAccountConnection updateConnection(
            ExAccountConnection existing,
            EncryptedConnectedId encryptedConnectedId
    ) {
        existing.replaceConnectedId(encryptedConnectedId);
        return existing;
    }
}
