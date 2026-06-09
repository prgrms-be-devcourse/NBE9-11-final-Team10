package com.team10.backend.domain.transaction.exception;

import com.team10.backend.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TransactionHistoryErrorCode implements ErrorCode {
    ;

    private final HttpStatus status;
    private final String message;
}
