package com.team10.backend.domain.exAccount.application.dto.res;

import com.team10.backend.domain.exAccount.domain.type.ExAccountConnectionStatus;

public record ExAccountConnectionRes(
        String organization,
        ExAccountConnectionStatus status
) {
}
