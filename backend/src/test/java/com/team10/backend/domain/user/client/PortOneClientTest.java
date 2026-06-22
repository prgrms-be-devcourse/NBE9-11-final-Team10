package com.team10.backend.domain.user.client;

import com.team10.backend.domain.user.exception.UserErrorCode;
import com.team10.backend.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortOneClientTest {

    @Mock RestClient restClient;

    PortOneClient portOneClient;

    @BeforeEach
    void setUp() {
        portOneClient = new PortOneClient("test-api-secret", restClient);
    }

    /** restClient.get()...body(PortOneIdentityVerification.class) 체인이 주어진 응답을 반환하도록 mock 체인을 구성한다. */
    private void mockHttpResponse(PortOneIdentityVerification response) {
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), any(Object[].class))).thenReturn(headersSpec);
        when(headersSpec.header(anyString(), anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(PortOneIdentityVerification.class)).thenReturn(response);
    }

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
            mockHttpResponse(response);

            PortOneIdentityVerification result = portOneClient.getIdentityVerification("identity-verification-id");

            assertThat(result.status()).isEqualTo("VERIFIED");
            assertThat(result.isVerified()).isTrue();
            assertThat(result.verifiedCustomer().name()).isEqualTo("홍길동");
        }

        @Test
        @DisplayName("status가 VERIFIED가 아니면 isVerified()는 false다")
        void notVerifiedStatus() {
            PortOneIdentityVerification response = new PortOneIdentityVerification(
                    "FAILED", null
            );
            mockHttpResponse(response);

            PortOneIdentityVerification result = portOneClient.getIdentityVerification("id");

            assertThat(result.isVerified()).isFalse();
        }

        @Test
        @DisplayName("응답이 null이면 IDENTITY_VERIFICATION_FAILED")
        void nullResponse_throwsBusinessException() {
            mockHttpResponse(null);

            assertThatThrownBy(() -> portOneClient.getIdentityVerification("id"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(UserErrorCode.IDENTITY_VERIFICATION_FAILED);
        }

        @Test
        @DisplayName("HTTP 호출 자체 실패 → IDENTITY_VERIFICATION_FAILED로 변환")
        void httpCallFailure_throwsBusinessException() {
            RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
            RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
            RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

            when(restClient.get()).thenReturn(uriSpec);
            when(uriSpec.uri(anyString(), any(Object[].class))).thenReturn(headersSpec);
            when(headersSpec.header(anyString(), anyString())).thenReturn(headersSpec);
            when(headersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(PortOneIdentityVerification.class))
                    .thenThrow(new RestClientException("connection refused"));

            assertThatThrownBy(() -> portOneClient.getIdentityVerification("id"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(UserErrorCode.IDENTITY_VERIFICATION_FAILED);
        }
    }
}
