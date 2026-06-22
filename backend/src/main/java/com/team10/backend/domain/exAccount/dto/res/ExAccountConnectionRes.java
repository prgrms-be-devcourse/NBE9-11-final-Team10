package com.team10.backend.domain.exAccount.dto.res;

import com.team10.backend.domain.exAccount.Type.ExAccountConnectionStatus;

public record ExAccountConnectionRes(
        String organization,
        ExAccountConnectionStatus status
) {
}
