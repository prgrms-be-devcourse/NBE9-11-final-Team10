package com.team10.backend.domain.exchange.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "korea-exim")
public record KoreaEximProperties(
        String baseUrl,
        String authKey
) {
}