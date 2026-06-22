package com.team10.backend.domain.codef.exAccount.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team10.backend.domain.codef.exAccount.config.CodefExAccountProperties;
import com.team10.backend.domain.codef.exAccount.dto.internal.CodefExAccountListRequest;
import com.team10.backend.domain.codef.exAccount.exception.CodefExAccountClientException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CodefExAccountClientTest {

    private static final String BASE_URL = "https://development.codef.io";
    private static final String ACCOUNT_LIST_PATH = "/v1/kr/bank/p/account/account-list";

    private MockRestServiceServer server;
    private CodefExAccountClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        CodefExAccountAuthClient authClient = mock(CodefExAccountAuthClient.class);
        when(authClient.getAccessToken()).thenReturn("account-access-token");
        client = new CodefExAccountClient(
                properties(),
                authClient,
                new CodefExAccountResponseDecoder(new ObjectMapper()),
                builder.build()
        );
    }

    @Test
    void requestsAccountListWithConnectedIdAndDecodesResponse() {
        String response = URLEncoder.encode("""
                {
                  "result": {"code": "CF-00000", "message": "성공"},
                  "data": {
                    "resDepositTrust": {
                      "resAccount": "1234567890",
                      "resAccountName": "입출금통장"
                    }
                  }
                }
                """, StandardCharsets.UTF_8);
        server.expect(requestTo(BASE_URL + ACCOUNT_LIST_PATH))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer account-access-token"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "organization": "0020",
                          "connectedId": "3Lj7J-OvQ",
                          "birthDate": "990101",
                          "withdrawAccountNo": "",
                          "withdrawAccountPassword": ""
                        }
                        """))
                .andRespond(withSuccess(response, MediaType.TEXT_PLAIN));

        JsonNode data = client.getAccountList(
                CodefExAccountListRequest.of("0020", "3Lj7J-OvQ", "990101"));

        assertThat(data.path("resDepositTrust").path("resAccount").asText())
                .isEqualTo("1234567890");
        server.verify();
    }

    @Test
    void retriesServerErrorOnlyOnce() {
        server.expect(requestTo(BASE_URL + ACCOUNT_LIST_PATH))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        server.expect(requestTo(BASE_URL + ACCOUNT_LIST_PATH))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> client.getAccountList(validRequest()))
                .isInstanceOf(CodefExAccountClientException.class)
                .hasMessage("CODEF 보유계좌 HTTP 요청에 실패했습니다.");
        server.verify();
    }

    @Test
    void doesNotRetryAuthenticationError() {
        server.expect(requestTo(BASE_URL + ACCOUNT_LIST_PATH))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.getAccountList(validRequest()))
                .isInstanceOf(CodefExAccountClientException.class)
                .hasMessage("CODEF 보유계좌 HTTP 요청에 실패했습니다.");
        server.verify();
    }

    @Test
    void rejectsMissingConnectedIdBeforeHttpRequest() {
        CodefExAccountListRequest request = CodefExAccountListRequest.of("0020", " ", "990101");

        assertThatThrownBy(() -> client.getAccountList(request))
                .isInstanceOf(CodefExAccountClientException.class)
                .hasMessage("CODEF 보유계좌 요청값이 올바르지 않습니다.")
                .hasMessageNotContaining("3Lj7J-OvQ");
        server.verify();
    }

    private CodefExAccountListRequest validRequest() {
        return CodefExAccountListRequest.of("0020", "3Lj7J-OvQ", "990101");
    }

    private CodefExAccountProperties properties() {
        return new CodefExAccountProperties(
                "DEMO",
                "account-client-id",
                "account-client-secret",
                "account-public-key",
                BASE_URL,
                ACCOUNT_LIST_PATH,
                "/v1/kr/bank/p/account/transaction-list"
        );
    }
}
