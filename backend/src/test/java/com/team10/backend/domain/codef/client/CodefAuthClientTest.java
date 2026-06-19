package com.team10.backend.domain.codef.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CodefAuthClientTest {

    @Mock CodefOAuthExchange codefOAuthExchange;

    CodefAuthClient codefAuthClient;

    @BeforeEach
    void setUp() {
        codefAuthClient = new CodefAuthClient("test-client-id", "test-client-secret", codefOAuthExchange);
    }

    @Nested
    @DisplayName("getAccessToken")
    class GetAccessToken {

        @Test
        @DisplayName("최초 호출 — OAuth API를 호출해 토큰을 발급받고 캐싱한다")
        void firstCall_fetchesAndCachesToken() {
            when(codefOAuthExchange.issueToken(anyString(), anyString()))
                    .thenReturn(new CodefOAuthExchange.CodefTokenResponse("abc123", 3600L));

            String token = codefAuthClient.getAccessToken();

            assertThat(token).isEqualTo("abc123");
            verify(codefOAuthExchange, times(1)).issueToken(anyString(), anyString());
        }

        @Test
        @DisplayName("캐시 유효기간 내 재호출 — API를 다시 호출하지 않고 캐시된 토큰을 반환한다")
        void secondCall_withinValidity_usesCachedToken() {
            when(codefOAuthExchange.issueToken(anyString(), anyString()))
                    .thenReturn(new CodefOAuthExchange.CodefTokenResponse("abc123", 3600L));

            String first = codefAuthClient.getAccessToken();
            String second = codefAuthClient.getAccessToken();

            assertThat(first).isEqualTo(second);
            verify(codefOAuthExchange, times(1)).issueToken(anyString(), anyString()); // 두 번째 호출은 캐시 재사용 — API 미호출
        }

        @Test
        @DisplayName("만료 5분 이내로 남은 캐시는 만료로 간주하고 재발급한다")
        void nearExpiry_refetchesToken() {
            // expires_in=60s → 만료 임박(<5분 버퍼)으로 즉시 무효 처리되어야 함
            when(codefOAuthExchange.issueToken(anyString(), anyString()))
                    .thenReturn(new CodefOAuthExchange.CodefTokenResponse("short-lived", 60L));
            String first = codefAuthClient.getAccessToken();

            when(codefOAuthExchange.issueToken(anyString(), anyString()))
                    .thenReturn(new CodefOAuthExchange.CodefTokenResponse("refreshed", 3600L));
            String second = codefAuthClient.getAccessToken();

            assertThat(first).isEqualTo("short-lived");
            assertThat(second).isEqualTo("refreshed");
            verify(codefOAuthExchange, times(2)).issueToken(anyString(), anyString());
        }

        @Test
        @DisplayName("OAuth 호출 자체가 실패하면 CodefAuthException으로 변환된다")
        void exchangeThrows_throwsCodefAuthException() {
            when(codefOAuthExchange.issueToken(anyString(), anyString()))
                    .thenThrow(new RuntimeException("network error"));

            assertThatThrownBy(() -> codefAuthClient.getAccessToken())
                    .isInstanceOf(CodefAuthException.class)
                    .hasMessageContaining("CODEF 토큰 응답 파싱 실패");
        }

        @Test
        @DisplayName("expires_in 필드가 없으면 CodefAuthException으로 변환된다")
        void missingExpiresInField_throwsCodefAuthException() {
            when(codefOAuthExchange.issueToken(anyString(), anyString()))
                    .thenReturn(new CodefOAuthExchange.CodefTokenResponse("abc123", null));

            assertThatThrownBy(() -> codefAuthClient.getAccessToken())
                    .isInstanceOf(CodefAuthException.class);
        }

        @Test
        @DisplayName("access_token 필드가 없으면 CodefAuthException으로 변환된다")
        void missingAccessTokenField_throwsCodefAuthException() {
            when(codefOAuthExchange.issueToken(anyString(), anyString()))
                    .thenReturn(new CodefOAuthExchange.CodefTokenResponse(null, 3600L));

            assertThatThrownBy(() -> codefAuthClient.getAccessToken())
                    .isInstanceOf(CodefAuthException.class);
        }
    }
}
