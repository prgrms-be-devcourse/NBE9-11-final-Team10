package com.team10.backend.domain.transfer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team10.backend.domain.transfer.dto.res.TransferRes;
import com.team10.backend.domain.transfer.entity.TransferIdempotency;
import com.team10.backend.domain.transfer.exception.TransferErrorCode;
import com.team10.backend.domain.transfer.repository.TransferIdempotencyRepository;
import com.team10.backend.domain.transfer.type.TransferStatus;
import com.team10.backend.domain.user.entity.User;
import com.team10.backend.domain.user.repository.UserRepository;
import com.team10.backend.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferIdempotencyServiceTest {

    @Mock
    private TransferIdempotencyRepository transferIdempotencyRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @InjectMocks
    private TransferIdempotencyService transferIdempotencyService;

    @Test
    @DisplayName("새 멱등성 키면 PROCESSING 레코드를 선점한다")
    void reserve_newKey_savesProcessingRecord() {
        User user = mock(User.class);
        TransferIdempotency idempotency = processing(user, "new-key", "request-hash");
        when(transferIdempotencyRepository.findByUser_IdAndIdempotencyKey(1L, "new-key"))
                .thenReturn(Optional.empty());
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(transferIdempotencyRepository.saveAndFlush(any(TransferIdempotency.class)))
                .thenReturn(idempotency);

        TransferIdempotencyReserveResult result = transferIdempotencyService.reserve(
                1L,
                "new-key",
                10L,
                "100200300002",
                50_000L,
                "점심값"
        );

        assertFalse(result.replay());
        assertSame(idempotency, result.idempotency());
        assertNull(result.storedResponse());
        verify(transferIdempotencyRepository).saveAndFlush(any(TransferIdempotency.class));
    }

    @Test
    @DisplayName("동시 동일 키 선점 충돌 후 기존 레코드가 PROCESSING이면 처리 중 예외로 변환한다")
    void reserve_uniqueViolationAndExistingProcessing_throwsProcessing() {
        String requestHash = requestHash(10L, "100200300002", 50_000L, "점심값");
        TransferIdempotency existing = processing(mock(User.class), "same-key", requestHash);
        when(transferIdempotencyRepository.findByUser_IdAndIdempotencyKey(1L, "same-key"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existing));
        when(userRepository.getReferenceById(1L)).thenReturn(mock(User.class));
        when(transferIdempotencyRepository.saveAndFlush(any(TransferIdempotency.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate idempotency key"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> transferIdempotencyService.reserve(
                        1L,
                        "same-key",
                        10L,
                        "100200300002",
                        50_000L,
                        "점심값"
                )
        );

        assertEquals(TransferErrorCode.IDEMPOTENCY_REQUEST_PROCESSING, exception.getErrorCode());
        verify(transferIdempotencyRepository).saveAndFlush(any(TransferIdempotency.class));
        verify(transferIdempotencyRepository, times(2))
                .findByUser_IdAndIdempotencyKey(1L, "same-key");
    }

    @Test
    @DisplayName("동시 동일 키 선점 충돌 후 기존 레코드가 SUCCESS이면 저장된 응답을 반환한다")
    void reserve_uniqueViolationAndExistingSuccess_returnsStoredResponse() throws Exception {
        String requestHash = requestHash(10L, "100200300002", 50_000L, "점심값");
        TransferRes storedResponse = new TransferRes(
                20L,
                TransferStatus.SUCCESS,
                10L,
                "100200300001",
                "100200300002",
                50_000L,
                50_000L,
                "점심값",
                LocalDateTime.of(2026, 6, 17, 10, 10)
        );
        TransferIdempotency existing = success(mock(User.class), "same-key", requestHash, storedResponse);
        when(transferIdempotencyRepository.findByUser_IdAndIdempotencyKey(1L, "same-key"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existing));
        when(userRepository.getReferenceById(1L)).thenReturn(mock(User.class));
        when(transferIdempotencyRepository.saveAndFlush(any(TransferIdempotency.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate idempotency key"));

        TransferIdempotencyReserveResult result = transferIdempotencyService.reserve(
                1L,
                "same-key",
                10L,
                "100200300002",
                50_000L,
                "점심값"
        );

        assertTrue(result.replay());
        assertNull(result.idempotency());
        assertEquals(storedResponse, result.storedResponse());
    }

    @Test
    @DisplayName("동시 동일 키 선점 충돌 후 기존 레코드와 요청 내용이 다르면 충돌 예외로 변환한다")
    void reserve_uniqueViolationAndDifferentRequest_throwsConflict() {
        String existingHash = requestHash(10L, "100200300002", 50_000L, "점심값");
        TransferIdempotency existing = processing(mock(User.class), "same-key", existingHash);
        when(transferIdempotencyRepository.findByUser_IdAndIdempotencyKey(1L, "same-key"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existing));
        when(userRepository.getReferenceById(1L)).thenReturn(mock(User.class));
        when(transferIdempotencyRepository.saveAndFlush(any(TransferIdempotency.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate idempotency key"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> transferIdempotencyService.reserve(
                        1L,
                        "same-key",
                        10L,
                        "100200300002",
                        60_000L,
                        "점심값"
                )
        );

        assertEquals(TransferErrorCode.IDEMPOTENCY_REQUEST_CONFLICT, exception.getErrorCode());
    }

    @Test
    @DisplayName("메모 공백이 다르면 다른 요청으로 판단한다")
    void reserve_sameKeyAndMemoWhitespaceDiffers_throwsConflict() {
        String existingHash = requestHash(10L, "100200300002", 50_000L, "점심값 ");
        TransferIdempotency existing = processing(mock(User.class), "same-key", existingHash);
        when(transferIdempotencyRepository.findByUser_IdAndIdempotencyKey(1L, "same-key"))
                .thenReturn(Optional.of(existing));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> transferIdempotencyService.reserve(
                        1L,
                        "same-key",
                        10L,
                        "100200300002",
                        50_000L,
                        "점심값"
                )
        );

        assertEquals(TransferErrorCode.IDEMPOTENCY_REQUEST_CONFLICT, exception.getErrorCode());
        verify(transferIdempotencyRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("멱등성 키가 없으면 필수 예외를 발생시킨다")
    void reserve_blankKey_throwsRequired() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> transferIdempotencyService.reserve(
                        1L,
                        " ",
                        10L,
                        "100200300002",
                        50_000L,
                        "점심값"
                )
        );

        assertEquals(TransferErrorCode.IDEMPOTENCY_KEY_REQUIRED, exception.getErrorCode());
        verify(transferIdempotencyRepository, never()).findByUser_IdAndIdempotencyKey(any(), any());
        verify(transferIdempotencyRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("멱등성 키가 100자를 초과하면 형식 예외를 발생시킨다")
    void reserve_tooLongKey_throwsInvalid() {
        String tooLongKey = "a".repeat(101);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> transferIdempotencyService.reserve(
                        1L,
                        tooLongKey,
                        10L,
                        "100200300002",
                        50_000L,
                        "점심값"
                )
        );

        assertEquals(TransferErrorCode.IDEMPOTENCY_KEY_INVALID, exception.getErrorCode());
        verify(transferIdempotencyRepository, never()).findByUser_IdAndIdempotencyKey(any(), any());
        verify(transferIdempotencyRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("멱등성 키에 허용되지 않은 문자가 있으면 형식 예외를 발생시킨다")
    void reserve_invalidCharacterKey_throwsInvalid() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> transferIdempotencyService.reserve(
                        1L,
                        "invalid key!",
                        10L,
                        "100200300002",
                        50_000L,
                        "점심값"
                )
        );

        assertEquals(TransferErrorCode.IDEMPOTENCY_KEY_INVALID, exception.getErrorCode());
        verify(transferIdempotencyRepository, never()).findByUser_IdAndIdempotencyKey(any(), any());
        verify(transferIdempotencyRepository, never()).saveAndFlush(any());
    }

    private TransferIdempotency processing(User user, String idempotencyKey, String requestHash) {
        return TransferIdempotency.processing(user, idempotencyKey, requestHash);
    }

    private TransferIdempotency success(
            User user,
            String idempotencyKey,
            String requestHash,
            TransferRes response
    ) throws Exception {
        TransferIdempotency idempotency = TransferIdempotency.processing(user, idempotencyKey, requestHash);
        idempotency.complete(null, objectMapper.writeValueAsString(response));
        ReflectionTestUtils.setField(idempotency, "id", 1L);
        return idempotency;
    }

    private String requestHash(Long senderAccountId, String receiverAccountNumber, Long amount, String memo) {
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
}
