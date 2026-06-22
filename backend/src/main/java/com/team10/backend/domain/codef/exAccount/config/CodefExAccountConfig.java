package com.team10.backend.domain.codef.exAccount.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CodefExAccountProperties.class)
public class CodefExAccountConfig {
}
