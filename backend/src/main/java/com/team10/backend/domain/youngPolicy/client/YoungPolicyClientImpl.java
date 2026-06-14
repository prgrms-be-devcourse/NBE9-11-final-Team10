package com.team10.backend.domain.youngPolicy.client;

import com.team10.backend.domain.youngPolicy.dto.req.YoungPolicyReq;
import com.team10.backend.domain.youngPolicy.dto.res.YoungPolicyRes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
@RequiredArgsConstructor // 추가: 스프링이 만들어둔 RestTemplate을 자동으로 연결해 줍니다.
public class YoungPolicyClientImpl implements YoungPolicyClient {

    // new RestTemplate() 부분을 지우고, 아래처럼 선언만 해둡니다.
    private final RestTemplate restTemplate;

    private static final String BASE_URL = "https://www.youthcenter.go.kr/opi/empList.do";

    @Override
    public YoungPolicyRes fetchPolicies(YoungPolicyReq request) {

        URI uri = UriComponentsBuilder.fromUriString(BASE_URL)
                .queryParam("apiKeyNm", request.apiKeyNm())
                .queryParam("pageNum", request.pageNum())
                .queryParam("pageSize", request.pageSize())
                .queryParam("rtnType", "json")
                .build()
                .toUri();

        return restTemplate.getForObject(uri, YoungPolicyRes.class);
    }
}