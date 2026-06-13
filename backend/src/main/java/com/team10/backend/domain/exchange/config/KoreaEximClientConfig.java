package com.team10.backend.domain.exchange.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(KoreaEximProperties.class)
public class KoreaEximClientConfig {

    @Bean
    public RestClient koreaEximRestClient(KoreaEximProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .build();
    }
}
