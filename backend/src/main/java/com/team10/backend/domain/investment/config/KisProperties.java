package com.team10.backend.domain.investment.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "kis")
public record KisProperties(
        @NotBlank String appKey,
        @NotBlank String appSecret
) {

}