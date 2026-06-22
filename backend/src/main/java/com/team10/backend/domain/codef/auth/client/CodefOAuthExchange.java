package com.team10.backend.domain.codef.auth.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.PostExchange;

/**
 * CODEF OAuth 토큰 발급 API — 선언적 HTTP 인터페이스.
 *
 * <p>실제 구현은 Spring이 런타임에 동적 프록시로 생성한다
 * ({@code com.team10.backend.domain.codef.auth.config.CodefHttpServiceConfig} 참고).
 * 토큰 캐싱/락 로직은 {@link CodefAuthClient}에 그대로 남기고, 이 인터페이스는
 * 순수 HTTP 호출 + 응답 역직렬화만 담당한다 (기존의 수동 {@code Map} 파싱을 대체).
 */
public interface CodefOAuthExchange {

    @PostExchange(url = "https://oauth.codef.io/oauth/token",
            contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    CodefTokenResponse issueToken(@RequestHeader("Authorization") String basicAuthHeader,
                                   @RequestBody String formBody);

    record CodefTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") Long expiresIn
    ) {}
}
