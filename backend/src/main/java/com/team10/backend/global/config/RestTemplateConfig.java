package com.team10.backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

// 타임 아웃 설정을 위해 RestTemplate을 직접 생성하여 스프링에 등록하는 설정 클래스
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        // 통신 설정을 담당하는 팩토리 객체를 직접 생성
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(5000); // 서버 연결 대기 시간
        factory.setReadTimeout(5000);    // 데이터 읽기 대기 시간

        // 설정이 완료된 팩토리를 넣어 RestTemplate을 완성하여 스프링에 등록합니다.
        return new RestTemplate(factory);
    }
}