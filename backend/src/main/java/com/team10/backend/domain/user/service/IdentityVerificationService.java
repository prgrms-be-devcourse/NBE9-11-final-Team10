package com.team10.backend.domain.user.service;

import com.team10.backend.domain.user.dto.req.OneWonStartReq;
import com.team10.backend.domain.user.dto.req.OneWonVerifyReq;
import com.team10.backend.domain.user.dto.res.OcrAcceptedRes;
import com.team10.backend.domain.user.dto.res.OneWonStartRes;
import com.team10.backend.domain.user.dto.res.OneWonVerifyRes;
import com.team10.backend.domain.user.entity.IdentityVerification;
import com.team10.backend.domain.user.entity.User;
import com.team10.backend.domain.user.exception.UserErrorCode;
import com.team10.backend.domain.user.ocr.OcrService;
import com.team10.backend.domain.user.repository.IdentityVerificationRepository;
import com.team10.backend.domain.user.repository.UserRepository;
import com.team10.backend.domain.user.type.VerificationStatus;
import com.team10.backend.domain.user.verification.BankCode;
import com.team10.backend.domain.user.verification.BankTransferService;
import com.team10.backend.domain.user.verification.OneWonVerificationService;
import com.team10.backend.global.exception.BusinessException;
import com.team10.backend.global.exception.GlobalErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 신분증 본인인증 3단계(OCR → 행안부 → 1원 송금)를 담당하는 서비스.
 *
 * <h2>단계별 흐름</h2>
 * <pre>
 * 1단계 — submitIdCardOcr()       : 신분증 이미지 접수 → 비동기 OCR + 행안부 검증
 * 3단계 — startOneWonVerification(): 1원 송금 요청 → 인증코드 Redis 저장
 * 3단계 — verifyOneWonCode()       : 코드 검증 → 완료 시 identityVerified=true
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IdentityVerificationService {

    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024L; // 10 MB
    private static final String OCR_DAILY_KEY_PREFIX = "identity:ocr:daily:";
    private static final int MAX_OCR_DAILY = 5;
    private static final Duration DAILY_TTL = Duration.ofDays(1);

    /** 최초 생성 시에만 EXPIRE를 설정하는 Lua 스크립트 (OneWonVerificationService와 동일 패턴) */
    private static final RedisScript<Long> INCR_WITH_EXPIRE_IF_NEW = RedisScript.of(
            "local v = redis.call('INCR', KEYS[1])\n" +
            "if v == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end\n" +
            "return v",
            Long.class
    );

    private final UserRepository userRepository;
    private final IdentityVerificationRepository identityVerificationRepository;
    private final OcrService ocrService;
    private final BankTransferService bankTransferService;
    private final OneWonVerificationService oneWonVerificationService;
    private final PlatformTransactionManager txManager;
    private final StringRedisTemplate redisTemplate;

    /**
     * 본인인증 1단계: 신분증 이미지를 접수하고 즉시 202를 반환한다.
     * OCR과 행안부 검증은 백그라운드에서 비동기 처리된다.
     */
    @Transactional
    public OcrAcceptedRes submitIdCardOcr(Long userId, MultipartFile imageFile) {
        validateImage(imageFile);
        checkOcrDailyLimit(userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        if (Boolean.TRUE.equals(user.getIdentityVerified())) {
            throw new BusinessException(UserErrorCode.IDENTITY_ALREADY_VERIFIED);
        }

        IdentityVerification verification = IdentityVerification.startOcr(user);
        IdentityVerification saved = identityVerificationRepository.save(verification);

        // 메인 스레드에서 바이트를 미리 읽음
        // MultipartFile은 요청 종료 시 Tomcat이 임시파일을 삭제하므로
        // afterCommit() 시점엔 이미 파일이 사라진다
        byte[] imageBytes;
        try {
            imageBytes = imageFile.getBytes();
        } catch (IOException e) {
            throw new BusinessException(UserErrorCode.OCR_IMAGE_REQUIRED);
        }

        Long verificationId = saved.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                ocrService.processAsync(imageBytes, verificationId);
            }
        });

        return new OcrAcceptedRes(
                saved.getId(),
                saved.getStatus(),
                "신분증 OCR 접수가 완료되었습니다. 처리 결과는 잠시 후 확인하실 수 있습니다."
        );
    }

    /**
     * 본인인증 3단계: 인증코드를 생성하고 사용자 계좌로 1원을 송금한다.
     *
     * <p>외부 API(bankTransferService.sendOneWon) 호출이 포함되므로 트랜잭션 외부에서 수행한다.
     * DB 조회 → Redis/외부 API → DB 쓰기 순으로 트랜잭션을 분리하여 커넥션 풀 고갈을 방지한다.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public OneWonStartRes startOneWonVerification(Long userId, OneWonStartReq request) {
        // Step 1: DB 조회 — Spring Data 자체 읽기 트랜잭션 사용
        IdentityVerification verification = identityVerificationRepository
                .findTopByUserIdOrderByCreatedAtDesc(userId)
                .filter(v -> v.getStatus() == VerificationStatus.GOVERNMENT_VERIFIED
                        || v.getStatus() == VerificationStatus.ONE_WON_PENDING)
                .orElseThrow(() -> new BusinessException(UserErrorCode.VERIFICATION_NOT_READY_FOR_ONE_WON));

        Long verificationId = verification.getId();

        // Step 2: 은행 점검 시간 검증
        validateBankAvailability(request.organization());

        // Step 3: Redis 코드 생성 + 외부 송금 API — 트랜잭션 없음
        String code = oneWonVerificationService.generateAndStore(verificationId, userId);
        try {
            bankTransferService.sendOneWon(request.organization(), request.accountNumber(), code);
        } catch (Exception e) {
            // 송금 실패 시 Redis 코드 + daily 카운터 모두 롤백
            oneWonVerificationService.deleteCode(verificationId);
            oneWonVerificationService.decrementDailyCount(userId);
            throw e;
        }

        // Step 4: DB 상태 업데이트 — 별도 쓰기 트랜잭션
        return new TransactionTemplate(txManager).execute(status -> {
            IdentityVerification managed = identityVerificationRepository
                    .findById(verificationId)
                    .orElseThrow(() -> new BusinessException(UserErrorCode.VERIFICATION_SESSION_NOT_FOUND));
            managed.startOneWon();
            return new OneWonStartRes(
                    managed.getId(),
                    managed.getStatus(),
                    "1원이 송금되었습니다. 입금 메모의 4자리 코드를 입력해주세요. (유효시간 10분)"
            );
        });
    }

    /**
     * 본인인증 3단계 코드 검증: 성공 시 identityVerified=true로 인증을 완료한다.
     */
    @Transactional
    public OneWonVerifyRes verifyOneWonCode(Long userId, OneWonVerifyReq request) {
        IdentityVerification verification = identityVerificationRepository
                .findTopByUserIdAndStatusOrderByCreatedAtDesc(userId, VerificationStatus.ONE_WON_PENDING)
                .orElseThrow(() -> new BusinessException(UserErrorCode.VERIFICATION_SESSION_NOT_FOUND));

        OneWonVerificationService.VerifyResult result =
                oneWonVerificationService.verify(verification.getId(), request.code());

        if (result == OneWonVerificationService.VerifyResult.EXPIRED) {
            throw new BusinessException(UserErrorCode.ONE_WON_CODE_EXPIRED);
        }
        if (result == OneWonVerificationService.VerifyResult.LOCKED) {
            throw new BusinessException(UserErrorCode.ONE_WON_ATTEMPT_EXCEEDED);
        }
        if (result == OneWonVerificationService.VerifyResult.MISMATCH) {
            throw new BusinessException(UserErrorCode.ONE_WON_CODE_MISMATCH);
        }

        verification.completeOneWon();

        User user = verification.getUser();
        user.completeIdentityVerification();

        return new OneWonVerifyRes(
                verification.getId(),
                verification.getStatus(),
                "본인인증이 완료되었습니다."
        );
    }

    private void validateBankAvailability(String organizationCode) {
        BankCode bank = BankCode.fromCode(organizationCode)
                .orElseThrow(() -> new BusinessException(UserErrorCode.UNSUPPORTED_BANK));

        LocalTime now = LocalTime.now(ZoneId.of("Asia/Seoul"));
        if (bank.isMaintenance(now)) {
            log.warn("[1원 인증] 은행 점검 시간 — bank={}, code={}, time={}", bank.getDisplayName(), organizationCode, now);
            throw new BusinessException(UserErrorCode.BANK_MAINTENANCE);
        }
    }

    private void checkOcrDailyLimit(Long userId) {
        String key = OCR_DAILY_KEY_PREFIX + userId;
        Long count = redisTemplate.execute(
                INCR_WITH_EXPIRE_IF_NEW,
                List.of(key),
                String.valueOf(DAILY_TTL.toSeconds())
        );
        if (count == null) {
            throw new BusinessException(GlobalErrorCode.INTERNAL_SERVER_ERROR);
        }
        if (count > MAX_OCR_DAILY) {
            log.warn("[OCR] 하루 요청 한도 초과 — userId={}, count={}", userId, count);
            throw new BusinessException(UserErrorCode.OCR_DAILY_LIMIT_EXCEEDED);
        }
        log.debug("[OCR] 일일 요청 카운트 — userId={}, count={}/{}", userId, count, MAX_OCR_DAILY);
    }

    private void validateImage(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            throw new BusinessException(UserErrorCode.OCR_IMAGE_REQUIRED);
        }
        if (imageFile.getSize() > MAX_IMAGE_SIZE) {
            throw new BusinessException(UserErrorCode.OCR_IMAGE_TOO_LARGE);
        }
        String contentType = imageFile.getContentType();
        if (contentType == null
                || (!contentType.equals("image/jpeg") && !contentType.equals("image/png"))) {
            throw new BusinessException(UserErrorCode.OCR_IMAGE_INVALID_TYPE);
        }
    }
}
