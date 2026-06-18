package com.team10.backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

// 타임 아웃 설정을 위해 RestTemplate을 직접 생성하여 스프링에 등록하는 설정 클래스
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClient() {
        // SimpleClientHttpRequestFactory(JDK 기본 URLConnection)는 커넥션 풀링/keep-alive가 없어 매 호출마다
        // TCP+TLS 핸드셰이크를 새로 한다. JDK HttpClient는 커넥션 풀을 내장하고 있어 동일 호스트로의 반복 호출
        // (PortOne, CODEF OAuth/OCR 등)에서 연결을 재사용한다.
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        // 무한 대기 방지를 위한 읽기 타임아웃 설정 (5초)
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(5));

        // 설정을 적용하여 RestClient 생성 및 등록
        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}