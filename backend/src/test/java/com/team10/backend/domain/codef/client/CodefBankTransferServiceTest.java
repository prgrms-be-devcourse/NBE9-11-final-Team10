package com.team10.backend.domain.codef.client;

import com.team10.backend.domain.user.exception.UserErrorCode;
import com.team10.backend.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CodefBankTransferServiceTest {

    @Mock CodefAuthClient codefAuthClient;
    @Mock RestClient codefBankTransferRestClient;

    @InjectMocks
    CodefBankTransferService service;

    private void mockHttpResponse(String body) {
        when(codefAuthClient.getAccessToken()).thenReturn("test-token");

        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(codefBankTransferRestClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.header(anyString(), anyString())).thenReturn(bodySpec);
        when(bodySpec.contentType(any(MediaType.class))).thenReturn(bodySpec);
        when(bodySpec.body(any(Object.class))).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(body);
    }

    @Nested
    @DisplayName("sendOneWon")
    class SendOneWon {

        @Test
        @DisplayName("정상 송금 — 예외 없이 완료")
        void success() {
            mockHttpResponse("{\"result\":{\"code\":\"CF-00000\",\"message\":\"SUCCESS\"}}");

            assertThatCode(() -> service.sendOneWon("004", "12345678901", "1234"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("CODEF 실패 코드 응답 → ONE_WON_TRANSFER_FAILED")
        void failureCode() {
            mockHttpResponse("{\"result\":{\"code\":\"CF-03002\",\"message\":\"계좌번호 오류\"}}");

            assertThatThrownBy(() -> service.sendOneWon("004", "12345678901", "1234"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(UserErrorCode.ONE_WON_TRANSFER_FAILED);
        }

        @Test
        @DisplayName("응답 본문이 JSON null → ONE_WON_TRANSFER_FAILED")
        void nullResponseBody() {
            mockHttpResponse("null");

            assertThatThrownBy(() -> service.sendOneWon("004", "12345678901", "1234"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(UserErrorCode.ONE_WON_TRANSFER_FAILED);
        }

        @Test
        @DisplayName("result 필드 누락 → ONE_WON_TRANSFER_FAILED")
        void missingResultField() {
            mockHttpResponse("{\"data\":{}}");

            assertThatThrownBy(() -> service.sendOneWon("004", "12345678901", "1234"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(UserErrorCode.ONE_WON_TRANSFER_FAILED);
        }

        @Test
        @DisplayName("토큰 발급 실패 → ONE_WON_TRANSFER_FAILED로 변환")
        void authFailure() {
            when(codefAuthClient.getAccessToken())
                    .thenThrow(new CodefAuthException("토큰 파싱 실패", new RuntimeException()));

            assertThatThrownBy(() -> service.sendOneWon("004", "12345678901", "1234"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(UserErrorCode.ONE_WON_TRANSFER_FAILED);
        }

        @Test
        @DisplayName("HTTP 호출 자체 실패 → ONE_WON_TRANSFER_FAILED로 변환")
        void httpCallFailure() {
            when(codefAuthClient.getAccessToken()).thenReturn("test-token");
            RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
            RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
            RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

            when(codefBankTransferRestClient.post()).thenReturn(uriSpec);
            when(uriSpec.uri(anyString())).thenReturn(bodySpec);
            when(bodySpec.header(anyString(), anyString())).thenReturn(bodySpec);
            when(bodySpec.contentType(any(MediaType.class))).thenReturn(bodySpec);
            when(bodySpec.body(any(Object.class))).thenReturn(bodySpec);
            when(bodySpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(String.class)).thenThrow(new RestClientException("read timeout"));

            assertThatThrownBy(() -> service.sendOneWon("004", "12345678901", "1234"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(UserErrorCode.ONE_WON_TRANSFER_FAILED);
        }
    }
}
