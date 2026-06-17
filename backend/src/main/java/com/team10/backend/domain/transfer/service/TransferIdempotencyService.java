package com.team10.backend.domain.transfer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team10.backend.domain.transfer.dto.res.TransferRes;
import com.team10.backend.domain.transfer.entity.Transfer;
import com.team10.backend.domain.transfer.entity.TransferIdempotency;
import com.team10.backend.domain.transfer.exception.TransferErrorCode;
import com.team10.backend.domain.transfer.repository.TransferIdempotencyRepository;
import com.team10.backend.domain.transfer.type.IdempotencyStatus;
import com.team10.backend.domain.user.entity.User;
import com.team10.backend.domain.user.repository.UserRepository;
import com.team10.backend.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TransferIdempotencyService {
//    - 키 검증
//    - requestHash 생성
//    - 기존 요청 상태 판단
//    - PROCESSING 선점 저장
//    - SUCCESS 완료 저장
//    - FAILED/EXPIRED 처리

    private final TransferIdempotencyRepository transferIdempotencyRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private static final Pattern IDEMPOTENCY_KEY_PATTERN = Pattern.compile("^[A-Za-z0-9._:-]{1,100}$");


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TransferIdempotencyReserveResult reserve(
            Long userId,
            String idempotencyKey,
            Long senderAccountId,
            String receiverAccountNumber,
            Long amount,
            String memo
    ) {
        validateIdempotencyKey(idempotencyKey);

        String requestHash = generateRequestHash(senderAccountId, receiverAccountNumber, amount, memo);

        return transferIdempotencyRepository
                .findByUser_IdAndIdempotencyKey(userId, idempotencyKey)
                .map(existing -> resolveExisting(existing, requestHash))
                .orElseGet(() -> createProcessing(userId, idempotencyKey, requestHash));

    }

    @Transactional
    public void completeSuccess(
            Long idempotencyId,
            Transfer transfer,
            TransferRes response
    ) {
        TransferIdempotency idempotency = transferIdempotencyRepository.findById(idempotencyId)
                .orElseThrow();

        idempotency.complete(transfer, serializeTransferResponse(response));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeFailure(
            Long idempotencyId
    ) {
        TransferIdempotency idempotency = transferIdempotencyRepository.findById(idempotencyId)
                .orElseThrow();

        idempotency.fail();
    }

    // 특정기한보다 오래된 TransferIdempotency 상태를 EXPIRE로 만료처리
    @Transactional
    public int expireStaleProcessing(Duration timeout) {
        LocalDateTime threshold = LocalDateTime.now().minus(timeout);

        List<TransferIdempotency> staleRecords =
                transferIdempotencyRepository.findStaleProcessing(threshold);

        staleRecords.forEach(TransferIdempotency::expire);

        return staleRecords.size();
    }

/*
    Race 처리 메서드
    1. 일단 PROCESSING insert를 시도한다.
    2. 바로 flush해서 unique violation이 즉시 발생하게 한다.
    3. unique violation이 나면 “누가 먼저 같은 키를 선점했다”라고 보고 기존 row를 다시 조회한다.
    4. 기존 row 상태를 보고 우리 비즈니스 예외/응답으로 바꾼다.
*/
    private TransferIdempotencyReserveResult createProcessing(
            Long userId,
            String idempotencyKey,
            String requestHash
    ) {
        try {
            User user = userRepository.getReferenceById(userId);

            TransferIdempotency idempotency = transferIdempotencyRepository.saveAndFlush(
                    TransferIdempotency.processing(user, idempotencyKey, requestHash)
            );

            return TransferIdempotencyReserveResult.reserved(idempotency);
        } catch (DataIntegrityViolationException e) { // 누가 먼저 같은 키를 선점한 경우
            TransferIdempotency existing = transferIdempotencyRepository
                    .findByUser_IdAndIdempotencyKey(userId, idempotencyKey)
                    .orElseThrow(() -> e);

            return resolveExisting(existing, requestHash);
        }
    }

    // 기존 상태 판단을 담당하는 메서드
    private TransferIdempotencyReserveResult resolveExisting(
            TransferIdempotency idempotency,
            String requestHash
    ) {
        // 멱등성 키 불일치 예외
        if (!idempotency.getRequestHash().equals(requestHash)) {
            throw new BusinessException(TransferErrorCode.IDEMPOTENCY_REQUEST_CONFLICT);
        }

        // 멱등성 키 처리 중
        if (idempotency.getStatus() == IdempotencyStatus.PROCESSING) {
            throw new BusinessException(TransferErrorCode.IDEMPOTENCY_REQUEST_PROCESSING);
        }

        // 완료 상태 -> 역직렬화된 DTO객체 반환
        if (idempotency.getStatus() == IdempotencyStatus.SUCCESS) {
            return TransferIdempotencyReserveResult.replay(
                    deserializeTransferResponse(idempotency.getResponseBody())
            );
        }

        // 기존 FAILED 멱등성 키 재요청 -> 실패 예외로 막힘
        if (idempotency.getStatus() == IdempotencyStatus.FAILED) {
            throw new BusinessException(TransferErrorCode.IDEMPOTENCY_REQUEST_FAILED);
        }

        // 만료된 키 -> 만료 예외로 막힘
        if (idempotency.getStatus() == IdempotencyStatus.EXPIRED) {
            throw new BusinessException(TransferErrorCode.IDEMPOTENCY_REQUEST_EXPIRED);
        }

        throw new BusinessException(TransferErrorCode.IDEMPOTENCY_REQUEST_PROCESSING);
    }

    private void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessException(TransferErrorCode.IDEMPOTENCY_KEY_REQUIRED);
        }

        if (!IDEMPOTENCY_KEY_PATTERN.matcher(idempotencyKey).matches()) {
            throw new BusinessException(TransferErrorCode.IDEMPOTENCY_KEY_INVALID);
        }
    }

    /*
    요청
    {
        "senderAccountId": 1,
            "receiverAccountNumber": "100200300002",
            "amount": 50000,
            "memo": "점심값"
    }
    -> 내부 문자열: 1|100200300002|50000|점심값
    -> SHA-256 해시로 바꿔 저장
*/
    // 요청 해시값 생성 메서드
    private String generateRequestHash(Long senderAccountId, String receiverAccountNumber, Long amount, String memo) {
        String normalizedMemo = memo == null ? "" : memo;

        String raw = String.join("|",
                String.valueOf(senderAccountId),
                receiverAccountNumber,
                String.valueOf(amount),
                normalizedMemo
        );

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }

    }

    // 직렬화 (자바객체 -> JSON)
    private String serializeTransferResponse(TransferRes response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize transfer response", e);
        }
    }

    // 역직렬화 (JSON -> 자바객체)
    private TransferRes deserializeTransferResponse(String responseBody) {
        try {
            return objectMapper.readValue(responseBody, TransferRes.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize transfer response", e);
        }
    }

}
