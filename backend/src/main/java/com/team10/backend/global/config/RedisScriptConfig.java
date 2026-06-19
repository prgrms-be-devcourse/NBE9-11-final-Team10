package com.team10.backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * 여러 도메인 서비스에 중복 정의돼 있던 Redis Lua 스크립트를 한 곳으로 모은 설정.
 *
 * <ul>
 *   <li>{@link #incrWithExpireIfNewScript()} — 최초 INCR 시에만 EXPIRE(일일 한도 카운터용).
 *       기존 {@code IdentityVerificationService}(OCR 일일 한도)와
 *       {@code OneWonVerificationService}(1원 인증 일일 한도)에 동일한 문자열로 중복 정의돼 있던 걸 통합.</li>
 *   <li>{@link #incrAndExpireScript()} — INCR + EXPIRE 매번 갱신(슬라이딩 윈도우 시도 횟수 카운터용).
 *       기존 {@code LoginAttemptService}(로그인 실패 횟수)와
 *       {@code OneWonVerificationService}(1원 인증 시도 횟수)에 동일한 문자열로 중복 정의돼 있던 걸 통합.</li>
 *   <li>{@link #getAndDeleteIfMatchScript()} — 값이 일치할 때만 원자적으로 삭제. Refresh Token
 *       Rotation 검증({@code RefreshTokenService})에서만 쓰이며 중복은 없었지만 일관성을 위해 함께 모음.</li>
 * </ul>
 *
 * {@link RedisScript}는 스크립트 본문과 결과 타입만 갖는 불변 객체라, 여러 서비스가 같은 빈을
 * 공유해도 스레드 안전성 문제가 없다.
 */
@Configuration
public class RedisScriptConfig {

    @Bean
    public RedisScript<Long> incrWithExpireIfNewScript() {
        return RedisScript.of(
                "local v = redis.call('INCR', KEYS[1])\n" +
                "if v == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end\n" +
                "return v",
                Long.class
        );
    }

    @Bean
    public RedisScript<Long> incrAndExpireScript() {
        return RedisScript.of(
                "local v = redis.call('INCR', KEYS[1])\n" +
                "redis.call('EXPIRE', KEYS[1], ARGV[1])\n" +
                "return v",
                Long.class
        );
    }

    @Bean
    public RedisScript<Long> getAndDeleteIfMatchScript() {
        return RedisScript.of(
                "local stored = redis.call('GET', KEYS[1])\n" +
                "if stored == ARGV[1] then\n" +
                "  redis.call('DEL', KEYS[1])\n" +
                "  return 1\n" +
                "else\n" +
                "  return 0\n" +
                "end",
                Long.class
        );
    }
}
