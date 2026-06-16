package com.team10.backend.domain.youngPolicy.client;

import com.team10.backend.domain.youngPolicy.dto.req.YoungPolicyReq;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class YoungPolicyClientImplTest {

    @Test
    void fetchPoliciesUsesConfiguredApiKey() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        YoungPolicyClientImpl client = new YoungPolicyClientImpl(builder.build(), "configured-api-key");

        server.expect(queryParam("apiKeyNm", "configured-api-key"))
                .andExpect(queryParam("pageNum", "1"))
                .andExpect(queryParam("pageSize", "10"))
                .andRespond(withSuccess("""
                        {"youthPolicyList":[]}
                        """, org.springframework.http.MediaType.APPLICATION_JSON));

        client.fetchPolicies(new YoungPolicyReq(1, 10));

        server.verify();
    }
}
