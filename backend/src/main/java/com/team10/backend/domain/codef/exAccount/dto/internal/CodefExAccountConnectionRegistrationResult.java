package com.team10.backend.domain.codef.exAccount.dto.internal;

import com.team10.backend.domain.codef.exAccount.type.CodefExAccountConnectionStatus;

public record CodefExAccountConnectionRegistrationResult(
        String organization,
        CodefExAccountConnectionStatus status
) {
}
