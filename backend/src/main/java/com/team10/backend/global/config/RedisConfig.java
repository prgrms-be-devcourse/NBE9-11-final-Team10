package com.team10.backend.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * Redis 연결 설정.
 *
 * 기존에는 spring-boot-starter-data-redis의 자동 설정에만 의존했음 — 그 경우 Lettuce의
 * 기본 commandTimeout(60초)이 그대로 적용된다. 로그인 실패 카운터({@code LoginAttemptService}),
 * 1원 인증 코드/시도/일일 한도({@code OneWonVerificationService}), Refresh Token
 * ({@code RefreshTokenService}), Access Token 블록리스트({@code TokenBlocklistService}),
 * OCR 일일 한도({@code IdentityVerificationService})까지 전부 요청 처리 스레드 안에서 Redis를
 * 동기 호출하므로, Redis 장애 시 요청 스레드가 60초씩 묶이는 걸 막기 위해 타임아웃을 짧게 명시한다.
 *
 * host/port는 {@code spring.data.redis.*} 프로퍼티를 그대로 사용한다(application-dev.yml,
 * application-test.yml에 설정돼 있음). application-prod.yml에는 아직 별도 값이 없어 기본값
 * (localhost:6379)으로 떨어지는 기존 동작은 이 클래스 도입으로 바뀌지 않음 — 운영 배포 전
 * SPRING_DATA_REDIS_HOST/PORT 환경변수 주입이 별도로 필요하다.
 */
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration(host, port);

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                // 명령 응답 대기 최대 시간. 기본값(60초)이면 Redis 장애 시 로그인/1원인증/OCR 요청 스레드가
                // 그만큼 묶여서 다른 요청까지 지연시킬 수 있어 짧게 잡는다.
                .commandTimeout(Duration.ofSeconds(3))
                .build();

        return new LettuceConnectionFactory(standaloneConfig, clientConfig);
    }

    @Bean
    public StringRedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        // 코드 전체가 카운터/플래그/Lua 스크립트 키로 문자열만 다룬다(ExchangeRateCacheRepository도
        // RedisTemplate<Object,Object> 대신 ObjectMapper로 직접 직렬화한 문자열을 저장함) — StringRedisTemplate로 충분.
        return new StringRedisTemplate(redisConnectionFactory);
    }
}
