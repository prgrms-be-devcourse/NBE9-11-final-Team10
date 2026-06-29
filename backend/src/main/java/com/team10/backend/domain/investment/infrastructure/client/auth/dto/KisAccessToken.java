package com.team10.backend.domain.investment.infrastructure.client.auth.dto;

import java.time.LocalDateTime;

public record KisAccessToken(
        String accessToken,
        LocalDateTime expiresAt
) {
}
