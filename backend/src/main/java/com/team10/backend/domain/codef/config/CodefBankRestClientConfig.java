package com.team10.backend.domain.codef.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

// CODEF 1원 송금(계좌인증) 전용 RestClient.
// 해당 API는 CODEF가 실제 은행에 동기 요청을 보내고 응답을 기다리는 구조라
// 전역 RestClientConfig의 5초 read timeout으로는 부족해 SocketTimeoutException이 발생함.
// 다른 CODEF 호출(OAuth 토큰 발급, OCR)은 5초로 충분히 동작하므로 전역 타임아웃은 유지하고
// 이 엔드포인트에만 더 긴 read timeout을 적용하기 위해 별도 빈으로 분리.
// JDK HttpClient는 커넥션 풀링/keep-alive를 내장 지원 — SimpleClientHttpRequestFactory와 달리 호출마다
// 새 TCP/TLS 핸드셰이크를 하지 않는다.
@Configuration
public class CodefBankRestClientConfig {

    @Bean
    public RestClient codefBankTransferRestClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(30)); // 1원 송금은 은행 응답 대기로 5초를 초과하는 경우가 있어 30초로 설정

        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}
