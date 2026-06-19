package com.team10.backend.domain.exAccount.exception;

import com.team10.backend.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ExAccountErrorCode implements ErrorCode {

    EX_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "외부 계좌를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
