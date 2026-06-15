package com.team10.backend.domain.user.ocr;

import com.team10.backend.domain.user.entity.IdentityVerification;
import com.team10.backend.domain.user.repository.IdentityVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * OCR 처리 결과를 DB에 저장하는 전용 서비스.
 *
 * <p>OcrService에서 self-injection(@Autowired @Lazy) 없이 @Transactional을 사용하기 위해 분리.
 * 각 메서드는 짧은 독립 트랜잭션으로 실행되어 CODEF API 대기 중 DB 커넥션을 점유하지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OcrPersistenceService {

    private final IdentityVerificationRepository identityVerificationRepository;

    @Transactional(readOnly = true)
    public IdentityVerification loadVerification(Long verificationId) {
        return identityVerificationRepository.findById(verificationId)
                .orElseGet(() -> {
                    log.error("[OCR] 인증 세션을 찾을 수 없음 — verificationId={}", verificationId);
                    return null;
                });
    }

    @Transactional
    public void saveOcrSuccess(Long verificationId, IdCardOcrResult result) {
        identityVerificationRepository.findById(verificationId).ifPresent(v ->
                v.completeOcr(result.name(), result.residentNumber(), result.issueDate())
        );
    }

    @Transactional
    public void saveGovSuccess(Long verificationId) {
        identityVerificationRepository.findById(verificationId).ifPresent(
                IdentityVerification::completeGovernmentVerification
        );
    }

    @Transactional
    public void saveFailure(Long verificationId, String reason) {
        identityVerificationRepository.findById(verificationId).ifPresent(v -> v.fail(reason));
    }
}
