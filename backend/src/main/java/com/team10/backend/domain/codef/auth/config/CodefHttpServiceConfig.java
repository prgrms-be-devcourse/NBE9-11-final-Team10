package com.team10.backend.domain.codef.config;

import com.team10.backend.domain.codef.client.CodefOAuthExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

// CodefAuthClient가 호출하는 CODEF OAuth 토큰 발급 API를 선언적 HTTP 인터페이스(CodefOAuthExchange)로
// 등록. 기존 RestClientConfig/CodefBankRestClientConfig와 동일하게 수동 @Bean 방식으로 통일해
// HttpServiceProxyFactory가 런타임에 동적 프록시를 생성하도록 한다.
@Configuration
public class CodefHttpServiceConfig {

    @Bean
    public CodefOAuthExchange codefOAuthExchange(RestClient restClient) {
        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build();
        return factory.createClient(CodefOAuthExchange.class);
    }
}
