package com.team10.backend.domain.codef.auth.config;

import com.team10.backend.domain.codef.auth.client.CodefAuthClient;
import com.team10.backend.domain.codef.auth.client.CodefBankTransferExchange;
import com.team10.backend.domain.codef.auth.client.CodefBearerTokenInterceptor;
import com.team10.backend.domain.codef.auth.client.CodefOAuthExchange;
import com.team10.backend.domain.codef.auth.ocr.CodefOcrExchange;
import com.team10.backend.domain.user.exception.UserErrorCode;
import com.team10.backend.global.exception.BusinessException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * CODEF 연동(OAuth 토큰 발급 / OCR / 1원송금) 전용 RestClient + 선언적 HTTP 인터페이스 등록.
 * 공유 베이스 RestClient({@link #codefBaseRestClient()})에서 목적별로 {@code mutate()}해서
 * 분기하는 구조로 통일했다(참고: <a href="https://tychejin.tistory.com/463">Spring Boot RestClient/HttpInterface</a>).
 *
 * <p>공유하는 것: connect timeout, JDK {@code HttpClient} 기반 요청 팩토리.</p>
 * <p>분기하는 것:</p>
 * <ul>
 *   <li>baseUrl — OAuth는 {@link CodefOAuthExchange#issueToken}의 {@code @PostExchange}가
 *       절대 URL(oauth.codef.io)을 직접 지정하므로 설정하지 않는다.</li>
 *   <li>Bearer 토큰 인터셉터 — OCR/1원송금에만 붙인다. OAuth는 토큰을 "발급받는" 호출 자체이므로
 *       절대 붙이지 않는다(붙이면 토큰 발급이 토큰 발급을 호출하는 순환 구조가 된다).</li>
 *   <li>read timeout — 1원송금은 은행 동기 응답 대기로 30초, 나머지는 5초.</li>
 *   <li>{@code defaultStatusHandler}의 에러코드 — 4xx/5xx를 각각 다른 {@link BusinessException}으로 변환한다.</li>
 * </ul>
 *
 * <p>OCR과 1원송금은 둘 다 {@code oneWonTransfer} 자격증명({@link CodefAuthClientConfig} 참고)으로
 * 발급된 토큰을 사용한다. CODEF DEMO 환경에서 자격증명 재사용이 거부되지 않음을 확인했다.</p>
 */
@Configuration(proxyBeanMethods = false)
public class CodefHttpServiceConfig {

    private static final String BASE_URL = "https://development.codef.io"; // 운영 전환 시 api 도메인으로 교체
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration BANK_TRANSFER_READ_TIMEOUT = Duration.ofSeconds(30); // 1원송금은 은행 응답 대기로 30초

    /** OAuth/OCR/1원송금이 공통으로 분기(mutate)하는 베이스 RestClient. baseUrl·인터셉터·에러 매핑은 없다. */
    @Bean
    public RestClient codefBaseRestClient() {
        return RestClient.builder()
                .requestFactory(requestFactory(DEFAULT_READ_TIMEOUT))
                .build();
    }

    private static ClientHttpRequestFactory requestFactory(Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(readTimeout);
        return factory;
    }

    // ------------------------------------------------------------------
    // OAuth 토큰 발급
    // ------------------------------------------------------------------

    @Bean
    public RestClient codefOAuthRestClient(RestClient codefBaseRestClient) {
        return oauthRestClientBuilder(codefBaseRestClient).build();
    }

    RestClient.Builder oauthRestClientBuilder(RestClient codefBaseRestClient) {
        return codefBaseRestClient.mutate()
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                    throw new BusinessException(UserErrorCode.CODEF_TOKEN_ISSUE_FAILED);
                });
    }

    @Bean
    public CodefOAuthExchange codefOAuthExchange(RestClient codefOAuthRestClient) {
        return proxy(codefOAuthRestClient, CodefOAuthExchange.class);
    }

    // ------------------------------------------------------------------
    // OCR — one-won-transfer 용 자격증명으로 발급된 토큰을 사용 (CodefAuthClientConfig 참고).
    // ------------------------------------------------------------------

    @Bean
    public RestClient codefOcrRestClient(
            RestClient codefBaseRestClient,
            @Qualifier("oneWonTransfer") CodefAuthClient codefAuthClient
    ) {
        return ocrRestClientBuilder(codefBaseRestClient, codefAuthClient).build();
    }

    RestClient.Builder ocrRestClientBuilder(RestClient codefBaseRestClient, CodefAuthClient codefAuthClient) {
        return codefBaseRestClient.mutate()
                .baseUrl(BASE_URL)
                .requestInterceptor(new CodefBearerTokenInterceptor(codefAuthClient))
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                    throw new BusinessException(UserErrorCode.OCR_FAILED);
                });
    }

    @Bean
    public CodefOcrExchange codefOcrExchange(RestClient codefOcrRestClient) {
        return proxy(codefOcrRestClient, CodefOcrExchange.class);
    }

    // ------------------------------------------------------------------
    // 1원송금 — one-won-transfer 용 자격증명. 은행 동기 응답 대기로 read timeout만 30초로 override.
    // ------------------------------------------------------------------

    @Bean
    public RestClient codefBankTransferRestClient(
            RestClient codefBaseRestClient,
            @Qualifier("oneWonTransfer") CodefAuthClient codefAuthClient
    ) {
        return bankTransferRestClientBuilder(codefBaseRestClient, codefAuthClient).build();
    }

    RestClient.Builder bankTransferRestClientBuilder(RestClient codefBaseRestClient, CodefAuthClient codefAuthClient) {
        return codefBaseRestClient.mutate()
                .baseUrl(BASE_URL)
                .requestFactory(requestFactory(BANK_TRANSFER_READ_TIMEOUT))
                .requestInterceptor(new CodefBearerTokenInterceptor(codefAuthClient))
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                    throw new BusinessException(UserErrorCode.ONE_WON_TRANSFER_FAILED);
                });
    }

    @Bean
    public CodefBankTransferExchange codefBankTransferExchange(RestClient codefBankTransferRestClient) {
        return proxy(codefBankTransferRestClient, CodefBankTransferExchange.class);
    }

    private static <T> T proxy(RestClient restClient, Class<T> exchangeType) {
        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build();
        return factory.createClient(exchangeType);
    }
}
