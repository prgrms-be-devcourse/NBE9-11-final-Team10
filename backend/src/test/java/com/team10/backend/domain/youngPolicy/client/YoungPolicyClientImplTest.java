package com.team10.backend.domain.youngPolicy.client;

import com.team10.backend.domain.youngPolicy.dto.req.YoungPolicyReq;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class YoungPolicyClientImplTest {

    @Test
    @DisplayName("현재 청년정책 API에 세션 쿠키와 JSON 본문으로 정책 목록을 요청한다")
    void fetchPoliciesRequestsCurrentPolicyApi() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        YoungPolicyClientImpl client = new YoungPolicyClientImpl(builder.build());

        server.expect(requestTo("https://www.youthcenter.go.kr"))
                .andExpect(method(GET))
                .andRespond(withSuccess("""
                        <html></html>
                        """, MediaType.TEXT_HTML)
                        .header(HttpHeaders.SET_COOKIE, "ygt=guest-token; Path=/; HttpOnly")
                        .header(HttpHeaders.SET_COOKIE, "XSRF-TOKEN=csrf-token; Path=/"));

        server.expect(requestTo("https://www.youthcenter.go.kr/wrk/yrm/plcyInfo/selectPlcy"))
                .andExpect(method(POST))
                .andExpect(header(HttpHeaders.COOKIE, containsString("ygt=guest-token")))
                .andExpect(header(HttpHeaders.COOKIE, containsString("XSRF-TOKEN=csrf-token")))
                .andExpect(jsonPath("$.plcyReq.useYn").value("Y"))
                .andExpect(jsonPath("$.plcyReq.plcyAprvSttsCd").value("0044002"))
                .andExpect(jsonPath("$.paggingVO.pageNum").value(1))
                .andExpect(jsonPath("$.paggingVO.pageSize").value(10))
                .andExpect(jsonPath("$.paggingVO.optorderBy").value(5))
                .andRespond(withSuccess("""
                        {
                          "resultCode": 200,
                          "resultMessage": "성공적으로 데이터를 가지고 왔습니다.",
                          "result": {
                            "plcyList": [
                              {
                                "plcyNo": "20260305005400112100",
                                "plcyNm": "청년문화예술패스",
                                "plcyExplnCn": "청년의 문화향유 기회를 제공",
                                "userLclsfNm": "금융/복지/문화",
                                "mclsfNm": "문화",
                                "sprtTrgtMinAge": 19,
                                "sprtTrgtMaxAge": 20,
                                "stdgCtpvSggCdList": "서울특별시",
                                "jobCd": "001",
                                "aplyPeriod": "20260225 ~ 20260630",
                                "aplyUrlAddr": "https://example.com",
                                "plcyAplyMthdCn": "온라인 신청"
                              }
                            ]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = client.fetchPolicies(new YoungPolicyReq(1, 10));

        assertThat(response.policyItems()).hasSize(1);
        assertThat(response.policyItems().get(0).plcyNo()).isEqualTo("20260305005400112100");

        server.verify();
    }

    @Test
    @DisplayName("세션 쿠키가 없으면 Cookie 헤더 없이 정책 목록을 요청한다")
    void fetchPoliciesOmitsCookieHeaderWhenGuestCookieIsBlank() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        YoungPolicyClientImpl client = new YoungPolicyClientImpl(builder.build());

        server.expect(requestTo("https://www.youthcenter.go.kr"))
                .andExpect(method(GET))
                .andRespond(withSuccess("<html></html>", MediaType.TEXT_HTML)
                        .header(HttpHeaders.SET_COOKIE, "   "));

        server.expect(requestTo("https://www.youthcenter.go.kr/wrk/yrm/plcyInfo/selectPlcy"))
                .andExpect(method(POST))
                .andExpect(request -> assertThat(request.getHeaders().containsHeader(HttpHeaders.COOKIE)).isFalse())
                .andRespond(withSuccess("""
                        {
                          "result": {
                            "plcyList": []
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = client.fetchPolicies(new YoungPolicyReq(1, 10));

        assertThat(response.policyItems()).isEmpty();

        server.verify();
    }

    @Test
    @DisplayName("Set-Cookie 값은 공백과 속성을 제거해 Cookie 헤더로 결합한다")
    void fetchPoliciesTrimsAndFiltersSetCookieValues() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        YoungPolicyClientImpl client = new YoungPolicyClientImpl(builder.build());

        server.expect(requestTo("https://www.youthcenter.go.kr"))
                .andExpect(method(GET))
                .andRespond(withSuccess("<html></html>", MediaType.TEXT_HTML)
                        .header(HttpHeaders.SET_COOKIE, "   ")
                        .header(HttpHeaders.SET_COOKIE, " ygt=guest-token ; Path=/; HttpOnly")
                        .header(HttpHeaders.SET_COOKIE, " XSRF-TOKEN=csrf-token ; Path=/"));

        server.expect(requestTo("https://www.youthcenter.go.kr/wrk/yrm/plcyInfo/selectPlcy"))
                .andExpect(method(POST))
                .andExpect(header(HttpHeaders.COOKIE, "ygt=guest-token; XSRF-TOKEN=csrf-token"))
                .andRespond(withSuccess("""
                        {
                          "result": {
                            "plcyList": []
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = client.fetchPolicies(new YoungPolicyReq(1, 10));

        assertThat(response.policyItems()).isEmpty();

        server.verify();
    }
}
