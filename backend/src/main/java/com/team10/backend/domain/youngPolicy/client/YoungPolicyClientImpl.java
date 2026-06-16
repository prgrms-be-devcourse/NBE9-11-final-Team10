package com.team10.backend.domain.youngPolicy.client;

import com.team10.backend.domain.youngPolicy.dto.req.YoungPolicyReq;
import com.team10.backend.domain.youngPolicy.dto.res.YoungPolicyExternalRes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class YoungPolicyClientImpl implements YoungPolicyClient {

    private static final String BASE_URL = "https://www.youthcenter.go.kr";
    private static final String POLICY_LIST_PATH = "/wrk/yrm/plcyInfo/selectPlcy";
    // 승인된 정책만 조회하는 청년센터 상태 코드입니다.
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

        // 청년센터 정책 API는 JSON 본문과 게스트 쿠키가 필요합니다.
        RestClient.RequestBodySpec requestSpec = restClient.post()
                .uri(BASE_URL + POLICY_LIST_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);

        // 쿠키가 없으면 빈 Cookie 헤더를 보내지 않습니다.
        if (StringUtils.hasText(cookie)) {
            requestSpec.header(HttpHeaders.COOKIE, cookie);
        }

        return requestSpec
                .body(YoungPolicyApiReq.from(request))
                .retrieve()
                .body(YoungPolicyExternalRes.class);
    }

    private String fetchGuestCookie() {
        // 메인 페이지에 먼저 접근해 게스트 쿠키를 받습니다.
        List<String> setCookies = restClient.get()
                .uri(BASE_URL)
                .retrieve()
                .toBodilessEntity()
                .getHeaders()
                .getOrEmpty(HttpHeaders.SET_COOKIE);

        // Cookie 헤더에는 Set-Cookie의 name=value 부분만 사용합니다.
        return setCookies.stream()
                .filter(StringUtils::hasText)
                .map(cookie -> cookie.split(";", 2)[0].trim())
                .filter(StringUtils::hasText)
                .reduce((left, right) -> left + "; " + right)
                .orElse("");
    }

    private record YoungPolicyApiReq(
            PolicySearchReq plcyReq,
            PagingReq paggingVO
    ) {
        private static YoungPolicyApiReq from(YoungPolicyReq request) {
            // 기본 동기화 대상은 사용 중인 승인 정책입니다.
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
