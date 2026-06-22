package com.team10.backend.domain.codef.exAccount.client;

import com.team10.backend.domain.codef.exAccount.config.CodefExAccountProperties;
import com.team10.backend.domain.codef.exAccount.exception.CodefExAccountAuthException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CodefExAccountAuthClientTest {

    private static final String OAUTH_TOKEN_URL = "https://oauth.codef.io/oauth/token";

    private MockRestServiceServer server;
    private CodefExAccountAuthClient authClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        authClient = new CodefExAccountAuthClient(properties(), builder.build());
    }

    @Test
    void issuesAccessTokenWithAccountInquiryCredentials() {
        String credentials = Base64.getEncoder().encodeToString(
                "account-client-id:account-client-secret".getBytes(StandardCharsets.UTF_8)
        );

        server.expect(requestTo(OAUTH_TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Basic " + credentials))
                .andExpect(content().contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string("grant_type=client_credentials&scope=read"))
                .andRespond(withSuccess("""
                        {
                          "access_token": "account-access-token",
                          "expires_in": 3600
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThat(authClient.getAccessToken()).isEqualTo("account-access-token");
        server.verify();
    }

    @Test
    void reusesCachedTokenForConcurrentRequests() {
        server.expect(requestTo(OAUTH_TOKEN_URL))
                .andRespond(withSuccess("""
                        {
                          "access_token": "cached-access-token",
                          "expires_in": 3600
                        }
                        """, MediaType.APPLICATION_JSON));

        List<CompletableFuture<String>> requests = IntStream.range(0, 10)
                .mapToObj(ignored -> CompletableFuture.supplyAsync(authClient::getAccessToken))
                .toList();

        assertThat(requests)
                .allSatisfy(request -> assertThat(request.join()).isEqualTo("cached-access-token"));
        server.verify();
    }

    @Test
    void rejectsInvalidOAuthResponse() {
        server.expect(requestTo(OAUTH_TOKEN_URL))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(authClient::getAccessToken)
                .isInstanceOf(CodefExAccountAuthException.class)
                .hasMessage("CODEF 외부계좌 OAuth 응답이 올바르지 않습니다.");
        server.verify();
    }

    private CodefExAccountProperties properties() {
        return new CodefExAccountProperties(
                "DEMO",
                "account-client-id",
                "account-client-secret",
                "account-public-key",
                "https://development.codef.io",
                "/account-list",
                "/transaction-list"
        );
    }
}
