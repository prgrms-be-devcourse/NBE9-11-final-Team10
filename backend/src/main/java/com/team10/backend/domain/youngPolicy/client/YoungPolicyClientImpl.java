package com.team10.backend.domain.youngPolicy.client;

import com.team10.backend.domain.youngPolicy.dto.req.YoungPolicyReq;
import com.team10.backend.domain.youngPolicy.dto.res.YoungPolicyExternalRes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
public class YoungPolicyClientImpl implements YoungPolicyClient {

    private final RestClient restClient;
    private final String apiKey;

    private static final String BASE_URL = "https://www.youthcenter.go.kr/opi/empList.do";

    public YoungPolicyClientImpl(
            RestClient restClient,
            @Value("${young-policy.api-key}") String apiKey
    ) {
        this.restClient = restClient;
        this.apiKey = apiKey;
    }

    @Override
    public YoungPolicyExternalRes fetchPolicies(YoungPolicyReq request) {
        java.util.Objects.requireNonNull(request, "request must not be null");

        URI uri = UriComponentsBuilder.fromUriString(BASE_URL)
                .queryParam("apiKeyNm", apiKey)
                .queryParam("pageNum", request.pageNum())
                .queryParam("pageSize", request.pageSize())
                .queryParam("rtnType", "json")
                .build()
                .toUri();

        return restClient.get()
                .uri(uri)
                .retrieve()
                .body(YoungPolicyExternalRes.class);
    }
}
