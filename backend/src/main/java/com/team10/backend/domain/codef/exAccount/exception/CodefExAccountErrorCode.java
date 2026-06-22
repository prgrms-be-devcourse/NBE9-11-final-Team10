package com.team10.backend.domain.codef.exAccount.exception;

import com.team10.backend.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CodefExAccountErrorCode implements ErrorCode {

    CODEF_CONNECTION_NOT_FOUND(HttpStatus.NOT_FOUND, "CODEF 외부계좌 연결정보를 찾을 수 없습니다."),
    CODEF_CONNECTION_INACTIVE(HttpStatus.CONFLICT, "CODEF 외부계좌 연결정보를 다시 인증해야 합니다.");

    private final HttpStatus status;
    private final String message;
}
