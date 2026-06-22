package com.team10.backend.domain.codef.exAccount.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.team10.backend.domain.codef.exAccount.config.CodefExAccountProperties;
import com.team10.backend.domain.codef.exAccount.exception.CodefExAccountAuthException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

import static com.team10.backend.domain.codef.exAccount.config.CodefExAccountRestClientConfig.OAUTH_REST_CLIENT;

@Component
public class CodefExAccountAuthClient {

    private static final String OAUTH_TOKEN_URL = "https://oauth.codef.io/oauth/token";
    private static final long TOKEN_REFRESH_SKEW_SECONDS = 300;

    private final CodefExAccountProperties properties;
    private final RestClient restClient;
    private final AtomicReference<TokenCache> tokenCache = new AtomicReference<>();
    private final Object tokenLock = new Object();

    public CodefExAccountAuthClient(
            CodefExAccountProperties properties,
            @Qualifier(OAUTH_REST_CLIENT) RestClient restClient
    ) {
        this.properties = properties;
        this.restClient = restClient;
    }

    public String getAccessToken() {
        TokenCache cache = tokenCache.get();
        if (isValid(cache)) {
            return cache.accessToken();
        }

        synchronized (tokenLock) {
            cache = tokenCache.get();
            if (isValid(cache)) {
                return cache.accessToken();
            }
            return issueAndCacheAccessToken();
        }
    }

    private boolean isValid(TokenCache cache) {
        return cache != null
                && Instant.now().getEpochSecond() < cache.expiresAtEpochSecond() - TOKEN_REFRESH_SKEW_SECONDS;
    }

    private String issueAndCacheAccessToken() {
        try {
            OAuthTokenRes response = restClient.post()
                    .uri(OAUTH_TOKEN_URL)
                    .header(HttpHeaders.AUTHORIZATION, createBasicAuthorization())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body("grant_type=client_credentials&scope=read")
                    .retrieve()
                    .body(OAuthTokenRes.class);

            validateResponse(response);

            long expiresAt = Instant.now().getEpochSecond() + response.expiresIn();
            tokenCache.set(new TokenCache(response.accessToken(), expiresAt));
            return response.accessToken();
        } catch (CodefExAccountAuthException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new CodefExAccountAuthException("CODEF 외부계좌 OAuth 요청에 실패했습니다.", exception);
        }
    }

    private String createBasicAuthorization() {
        String credentials = properties.clientId() + ":" + properties.clientSecret();
        String encoded = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }

    private void validateResponse(OAuthTokenRes response) {
        if (response == null
                || response.accessToken() == null
                || response.accessToken().isBlank()
                || response.expiresIn() == null
                || response.expiresIn() <= TOKEN_REFRESH_SKEW_SECONDS) {
            throw new CodefExAccountAuthException("CODEF 외부계좌 OAuth 응답이 올바르지 않습니다.");
        }
    }

    private record OAuthTokenRes(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") Long expiresIn
    ) {
    }

    private record TokenCache(String accessToken, long expiresAtEpochSecond) {
    }
}
