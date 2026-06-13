package com.team10.backend.domain.exchange.client;

import com.team10.backend.domain.exchange.config.KoreaEximProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class KoreaEximExchangeRateClient {

    private final RestClient koreaEximRestClient;
    private final KoreaEximProperties properties;

    public List<KoreaEximExchangeRateRes> fetch(LocalDate date) {
        return koreaEximRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/site/program/financial/exchangeJSON")
                        .queryParam("authkey", properties.authKey())
                        .queryParam("searchdate", date.format(DateTimeFormatter.BASIC_ISO_DATE))
                        .queryParam("data", "AP01") // 현재 환율API의 API 타입 코드
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

}
