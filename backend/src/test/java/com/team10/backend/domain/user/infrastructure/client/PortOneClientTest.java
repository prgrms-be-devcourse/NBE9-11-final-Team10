package com.team10.backend.domain.user.infrastructure.client;
import com.team10.backend.domain.user.infrastructure.config.PortOneHttpServiceConfigTest;

import com.team10.backend.domain.user.domain.exception.UserErrorCode;
import com.team10.backend.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * HTTP 호출 자체(baseUrl/헤더/타임아웃/4xx·5xx 변환)는 PortOneIdentityVerificationExchange +
 * PortOneHttpServiceConfig로 위임됐다 — 그 동작은 {@link PortOneHttpServiceConfigTest}에서
 * MockRestServiceServer로 검증한다. 여기서는 PortOneClient가 그 위에서 하는
 * null 응답 처리 / 연결 실패(ResourceAccessException) 변환만 검증한다.
 */
@ExtendWith(MockitoExtension.class)
public class PortOneClientTest {

    @Mock
    PortOneIdentityVerificationExchange exchange;

    @InjectMocks
    PortOneClient portOneClient;

    @Nested
    @DisplayName("getIdentityVerification")
    class GetIdentityVerification {

        @Test
        @DisplayName("정상 응답 — 인증 결과를 그대로 반환한다")
        void success() {
            PortOneIdentityVerification response = new PortOneIdentityVerification(
                    "VERIFIED",
                    new PortOneIdentityVerification.VerifiedCustomer("홍길동", "1990-01-01", "01012345678")
            );
            when(exchange.getIdentityVerification("identity-verification-id")).thenReturn(response);

            PortOneIdentityVerification result = portOneClient.getIdentityVerification("identity-verification-id");

            assertThat(result.status()).isEqualTo("VERIFIED");
            assertThat(result.isVerified()).isTrue();
            assertThat(result.verifiedCustomer().name()).isEqualTo("홍길동");
        }

        @Test
        @DisplayName("status가 VERIFIED가 아니면 isVerified()는 false다")
        void notVerifiedStatus() {
            PortOneIdentityVerification response = new PortOneIdentityVerification("FAILED", null);
            when(exchange.getIdentityVerification("id")).thenReturn(response);

            PortOneIdentityVerification result = portOneClient.getIdentityVerification("id");

            assertThat(result.isVerified()).isFalse();
        }

        @Test
        @DisplayName("응답이 null이면 IDENTITY_VERIFICATION_FAILED")
        void nullResponse_throwsBusinessException() {
            when(exchange.getIdentityVerification("id")).thenReturn(null);

            assertThatThrownBy(() -> portOneClient.getIdentityVerification("id"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(UserErrorCode.IDENTITY_VERIFICATION_FAILED);
        }

        @Test
        @DisplayName("타임아웃/연결거부 등 응답 자체가 없는 실패 → IDENTITY_VERIFICATION_FAILED로 변환")
        void connectionFailure_throwsBusinessException() {
            when(exchange.getIdentityVerification("id"))
                    .thenThrow(new ResourceAccessException("connection refused"));

            assertThatThrownBy(() -> portOneClient.getIdentityVerification("id"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(UserErrorCode.IDENTITY_VERIFICATION_FAILED);
        }
    }
}
