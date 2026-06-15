package com.team10.backend.domain.user.ocr;

import com.team10.backend.domain.user.entity.IdentityVerification;
import com.team10.backend.domain.user.verification.GovernmentVerifyResult;
import com.team10.backend.domain.user.verification.GovernmentVerifyTimeoutException;
import com.team10.backend.domain.user.verification.MockGovernmentVerifyService;
import com.team10.backend.domain.user.verification.VerificationSessionRecorder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * CODEF OCR API 기반 신분증 인증 비동기 처리 서비스 (1단계 → 2단계 즉시 체이닝).
 *
 * <h2>처리 흐름</h2>
 * <pre>
 * [ocrExecutor 스레드]
 *   1. 이미지 바이트 → CODEF OCR API → 구조화된 응답 [이름, 주민번호, 발급일자]
 *      └─ OCR 실패 → FAILED 저장, 종료
 *   2. [즉시 체이닝] MockGovernmentVerifyService.verify()
 *      ├─ VERIFIED            → GOVERNMENT_VERIFIED (3단계 대기)
 *      ├─ ISSUE_DATE_MISMATCH → FAILED
 *      └─ IDENTITY_NOT_FOUND  → FAILED
 * </pre>
 *
 * <p>CODEF API 호출(느린 외부 I/O) 동안 DB 커넥션을 점유하지 않도록
 * DB 저장은 {@link OcrPersistenceService}에 위임한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OcrService {

    private final CodefOcrClient codefOcrClient;
    private final OcrPersistenceService ocrPersistenceService;
    private final MockGovernmentVerifyService mockGovernmentVerifyService;
    private final VerificationSessionRecorder verificationSessionRecorder;

    @Async("ocrExecutor")
    public void processAsync(byte[] imageBytes, Long verificationId) {
        log.info("[OCR] 1단계 시작 — verificationId={}, thread={}", verificationId, Thread.currentThread().getName());

        IdentityVerification verification = ocrPersistenceService.loadVerification(verificationId);
        if (verification == null) return;

        try {
            // ── 1단계: CODEF OCR (트랜잭션 밖) ─────────────────────────────
            IdCardOcrResult result = codefOcrClient.extractIdCard(imageBytes);

            ocrPersistenceService.saveOcrSuccess(verificationId, result);
            log.info("[OCR] 1단계 완료 — verificationId={}, name={}", verificationId, result.name());

            // ── 2단계: 행안부 진위 확인 즉시 체이닝 ────────────────────────
            chainGovernmentVerification(verificationId, result);

        } catch (Exception e) {
            ocrPersistenceService.saveFailure(verificationId, "이미지 처리 중 오류가 발생했습니다: " + e.getMessage());
            log.error("[OCR] 처리 오류 — verificationId={}", verificationId, e);
        }
    }

    private void chainGovernmentVerification(Long verificationId, IdCardOcrResult result) {
        log.info("[GOV] 2단계 시작 — verificationId={}", verificationId);

        try {
            GovernmentVerifyResult govResult = mockGovernmentVerifyService.verify(
                    result.name(), result.residentNumber(), result.issueDate()
            );

            switch (govResult) {
                case VERIFIED -> {
                    ocrPersistenceService.saveGovSuccess(verificationId);
                    log.info("[GOV] 2단계 완료 — verificationId={}", verificationId);
                }
                case ISSUE_DATE_MISMATCH -> {
                    ocrPersistenceService.saveFailure(verificationId, "분실·도난 신분증 의심: 발급일자가 정부 기록과 일치하지 않습니다.");
                    log.warn("[GOV] 발급일자 불일치 — verificationId={}", verificationId);
                }
                case IDENTITY_NOT_FOUND -> {
                    ocrPersistenceService.saveFailure(verificationId, "존재하지 않는 명의: 위조 신분증이 의심됩니다.");
                    log.warn("[GOV] 존재하지 않는 명의 — verificationId={}", verificationId);
                }
            }

        } catch (GovernmentVerifyTimeoutException e) {
            log.error("[GOV] 타임아웃 — verificationId={}", verificationId, e);
            verificationSessionRecorder.markFailedInNewTransaction(
                    verificationId,
                    "행안부 연동 타임아웃: 잠시 후 다시 시도해주세요."
            );
        }
    }
}
