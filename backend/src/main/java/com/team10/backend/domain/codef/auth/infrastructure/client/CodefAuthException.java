package com.team10.backend.domain.codef.auth.infrastructure.client;

import com.team10.backend.domain.codef.auth.infrastructure.ocr.CodefOcrClient;

/** CODEF OAuth 토큰 발급/파싱 실패 예외. */
public class CodefAuthException extends RuntimeException {

    public CodefAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
