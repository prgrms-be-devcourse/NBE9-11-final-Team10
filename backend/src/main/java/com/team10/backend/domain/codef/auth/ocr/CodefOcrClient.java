package com.team10.backend.domain.codef.auth.ocr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team10.backend.domain.codef.auth.client.CodefApiResult;
import com.team10.backend.domain.codef.auth.client.CodefAuthException;
import com.team10.backend.domain.codef.auth.ocr.CodefOcrExchange.CodefOcrData;
import com.team10.backend.domain.codef.auth.ocr.CodefOcrExchange.CodefOcrRequest;
import com.team10.backend.domain.codef.auth.ocr.CodefOcrExchange.CodefOcrResponse;
import com.team10.backend.domain.user.exception.UserErrorCode;
import com.team10.backend.global.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** CODEF 신분증 OCR 클라이언트. */
@Slf4j
@Component
public class CodefOcrClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final CodefOcrExchange codefOcrExchange;

    public CodefOcrClient(CodefOcrExchange codefOcrExchange) {
        this.codefOcrExchange = codefOcrExchange;
    }

    /**
     * 신분증 이미지 바이트를 CODEF OCR API로 전송하고 구조화된 정보를 반환한다.
     */
    public IdCardOcrResult extractIdCard(byte[] imageBytes) {
        CodefOcrResponse response = requestOcr(imageBytes);

        CodefApiResult result = (response != null) ? response.result() : null;
        String code = (result != null) ? result.code() : null;
        log.debug("[CODEF OCR] 응답 수신 — code={}", code);

        if (!"CF-00000".equals(code)) {
            log.error("[CODEF OCR] 실패 — code={}, message={}", code, result != null ? result.message() : null);
            throw new BusinessException(UserErrorCode.OCR_FAILED);
        }

        CodefOcrData data = (response != null) ? response.data() : null;
        if (data == null) {
            throw new BusinessException(UserErrorCode.OCR_FAILED);
        }

        String name        = data.resUserName();
        String rawIdentity = data.resUserIdentity();
        String rawDate     = data.resIssueDate();

        if (isBlank(name) || isBlank(rawIdentity) || isBlank(rawDate)
                || rawIdentity.length() < 13 || rawDate.length() < 8) {
            log.warn("[CODEF OCR] 필수 필드 누락 — name={}, identity={}, date={}", name, maskIdentity(rawIdentity), rawDate);
            throw new BusinessException(UserErrorCode.OCR_FAILED);
        }

        String residentNumber = rawIdentity.substring(0, 6) + "-" + rawIdentity.substring(6);
        String issueDate = rawDate.substring(0, 4) + "-" + rawDate.substring(4, 6) + "-" + rawDate.substring(6, 8);

        log.info("[CODEF OCR] 추출 완료 — name={}", name);
        return new IdCardOcrResult(name, residentNumber, issueDate);
    }

    /** OCR API 호출 + 응답 디코딩(URL-인코딩 바디라 String으로 받아 직접 디코딩). 실패 시 OCR_FAILED. */
    private CodefOcrResponse requestOcr(byte[] imageBytes) {
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        CodefOcrRequest body = new CodefOcrRequest("0", "0", base64Image, "0", "0");

        try {
            String response = codefOcrExchange.requestOcr(body);
            String decoded = URLDecoder.decode(response != null ? response : "", StandardCharsets.UTF_8);
            return OBJECT_MAPPER.readValue(decoded, CodefOcrResponse.class);

        } catch (CodefAuthException | RestClientException | IllegalArgumentException | JsonProcessingException e) {
            log.error("[CODEF OCR] 처리 오류", e);
            throw new BusinessException(UserErrorCode.OCR_FAILED);
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** 주민등록번호 등 식별 정보를 로그용으로 마스킹한다 (앞 6자리만 노출). */
    private String maskIdentity(String raw) {
        if (raw == null || raw.length() < 6) {
            return "***";
        }
        return raw.substring(0, 6) + "-*******";
    }
}
