package com.team10.backend.domain.youngPolicy.client;

import com.team10.backend.domain.youngPolicy.dto.req.YoungPolicyReq;
import com.team10.backend.domain.youngPolicy.dto.res.YoungPolicyRes;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
public class YoungPolicyClientImpl implements YoungPolicyClient {

    //스프링에서 제공하는 RestTemplate을 사용하여 외부 API와 통신
    private final RestTemplate restTemplate = new RestTemplate();

    // 외부 API의 기본 주소를 입력합니다. (실제 요청할 API 주소로 수정이 필요합니다)
    private static final String BASE_URL = "https://www.youthcenter.go.kr/opi/empList.do";

    @Override
    public YoungPolicyRes fetchPolicies(YoungPolicyReq request) {

        // 요청 주소(URL) 뒤에 Req 검색 조건 파라미터들을 추가해서 조립
        URI uri = UriComponentsBuilder.fromUriString(BASE_URL)
                .queryParam("apiKeyNm", request.apiKeyNm())
                .queryParam("pageNum", request.pageNum())
                .queryParam("pageSize", request.pageSize())
                .queryParam("rtnType", "json") // 결과를 JSON 형태로 받기 위한 고정 값입니다.
                .build()
                .toUri();

        // 2. 조립된 주소로 요청을 보내고, 결과를 YoungPolicyRes 형태로 받아옵니다.
        return restTemplate.getForObject(uri, YoungPolicyRes.class);
    }
}
