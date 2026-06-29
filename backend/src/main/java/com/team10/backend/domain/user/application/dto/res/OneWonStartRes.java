package com.team10.backend.domain.user.application.dto.res;

import com.team10.backend.domain.user.domain.type.VerificationStatus;

public record OneWonStartRes(
        Long verificationId,
        VerificationStatus status,
        String message
) {}
