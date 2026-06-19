package com.team10.backend.domain.codef.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

/** CODEF OAuth Access Token 발급/캐싱 (OCR, 1원 송금 등 CODEF API 호출에서 공용으로 사용) */
@Slf4j
@Component
public class CodefAuthClient {

    private final String clientId;
    private final String clientSecret;
    // 실제 OAuth POST 호출은 선언적 HTTP 인터페이스(CodefOAuthExchange)로 위임 — 이 클래스는
    // 토큰 캐싱/락 로직만 담당한다 (CodefHttpServiceConfig에서 빈으로 등록).
    private final CodefOAuthExchange codefOAuthExchange;

    // AT 캐시 — 만료 5분 전 갱신, token+만료시간 원자적 관리
    private record TokenCache(String token, long expiryEpoch) {}
    private final AtomicReference<TokenCache> tokenCache = new AtomicReference<>();
    private final Object tokenLock = new Object();

    public CodefAuthClient(
            @Value("${codef.client-id}") String clientId,
            @Value("${codef.client-secret}") String clientSecret,
            CodefOAuthExchange codefOAuthExchange
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.codefOAuthExchange = codefOAuthExchange;
    }

    /** 캐시된 토큰이 유효하면 재사용, 만료 5분 이내로 남았으면 새로 발급한다 */
    public String getAccessToken() {
        TokenCache cache = tokenCache.get();
        if (isValid(cache)) {
            return cache.token();
        }

        // Double-Checked Locking — 동시 만료 시 여러 스레드가 동시에 OAuth API를 호출하는 것을 방지
        synchronized (tokenLock) {
            cache = tokenCache.get();
            if (isValid(cache)) {
                return cache.token();
            }
            return fetchAndCacheToken();
        }
    }

    private boolean isValid(TokenCache cache) {
        return cache != null && Instant.now().getEpochSecond() < cache.expiryEpoch() - 300;
    }

    private String fetchAndCacheToken() {
        try {
            String credentials = Base64.getEncoder().encodeToString(
                    (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

            CodefOAuthExchange.CodefTokenResponse response = codefOAuthExchange.issueToken(
                    "Basic " + credentials,
                    "grant_type=client_credentials&scope=read"
            );

            if (response == null || response.accessToken() == null || response.expiresIn() == null) {
                throw new IllegalStateException("access_token 또는 expires_in 누락");
            }

            long expiryEpoch = Instant.now().getEpochSecond() + response.expiresIn();
            tokenCache.set(new TokenCache(response.accessToken(), expiryEpoch));
            log.info("[CODEF] 토큰 발급 완료");
            return response.accessToken();
        } catch (Exception e) {
            // 호출부에서 각자의 도메인 예외로 변환하도록 그대로 전파 (BusinessException이면 호출부에서 가로채지 못함)
            throw new CodefAuthException("CODEF 토큰 응답 파싱 실패", e);
        }
    }
}
