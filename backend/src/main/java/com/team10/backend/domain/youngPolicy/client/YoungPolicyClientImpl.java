package com.team10.backend.domain.youngPolicy.client;

import com.team10.backend.domain.youngPolicy.dto.req.YoungPolicyReq;
import com.team10.backend.domain.youngPolicy.dto.res.YoungPolicyExternalRes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class YoungPolicyClientImpl implements YoungPolicyClient {

    private static final String BASE_URL = "https://www.youthcenter.go.kr";
    private static final String POLICY_LIST_PATH = "/wrk/yrm/plcyInfo/selectPlcy";
    private static final String POLICY_APPROVED_STATUS = "0044002";
    private static final int DEFAULT_ORDER_BY = 5;

    private final RestClient restClient;

    public YoungPolicyClientImpl(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public YoungPolicyExternalRes fetchPolicies(YoungPolicyReq request) {
        java.util.Objects.requireNonNull(request, "request must not be null");
        String cookie = fetchGuestCookie();

        return restClient.post()
                .uri(BASE_URL + POLICY_LIST_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.COOKIE, cookie)
                .body(YoungPolicyApiReq.from(request))
                .retrieve()
                .body(YoungPolicyExternalRes.class);
    }

    private String fetchGuestCookie() {
        List<String> setCookies = restClient.get()
                .uri(BASE_URL)
                .retrieve()
                .toBodilessEntity()
                .getHeaders()
                .getOrEmpty(HttpHeaders.SET_COOKIE);

        return setCookies.stream()
                .map(cookie -> cookie.split(";", 2)[0])
                .reduce((left, right) -> left + "; " + right)
                .orElse("");
    }

    private record YoungPolicyApiReq(
            PolicySearchReq plcyReq,
            PagingReq paggingVO
    ) {
        private static YoungPolicyApiReq from(YoungPolicyReq request) {
            return new YoungPolicyApiReq(
                    new PolicySearchReq("Y", POLICY_APPROVED_STATUS),
                    new PagingReq(request.pageNum(), request.pageSize(), DEFAULT_ORDER_BY)
            );
        }
    }

    private record PolicySearchReq(
            String useYn,
            String plcyAprvSttsCd
    ) {
    }

    private record PagingReq(
            Integer pageNum,
            Integer pageSize,
            Integer optorderBy
    ) {
    }
}
