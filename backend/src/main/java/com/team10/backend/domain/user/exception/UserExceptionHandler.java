package com.team10.backend.domain.user.exception;

import com.team10.backend.global.exception.ErrorResponse;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * domain.user 전용 예외 처리기.
 *
 * <p>{@code GlobalExceptionHandler}는 domain 패키지에 의존하지 않는 공통 처리기로 유지하고,
 * user 도메인에서만 의미가 확정되는 기술 예외(JwtException 등)는 여기서 번역한다.
 * (예: JwtException은 {@code UserService.refresh()}에서만 컨트롤러까지 전파되며, 항상
 * 만료/위조된 refresh 요청을 의미한다 — {@code JwtAuthenticationFilter}의 동일 예외는
 * Security 필터 단계에서 별도로 처리되어 여기에 도달하지 않는다.)
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.team10.backend.domain.user")
public class UserExceptionHandler {

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ErrorResponse> handleJwtException(JwtException e) {
        log.warn("[JWT] refresh 토큰 파싱 실패: {}", e.getMessage());
        UserErrorCode code = UserErrorCode.INVALID_REFRESH_TOKEN;
        return ResponseEntity.status(code.getStatus()).body(ErrorResponse.from(code));
    }
}
