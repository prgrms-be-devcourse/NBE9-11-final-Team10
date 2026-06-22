package com.team10.backend.domain.codef.exAccount.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.team10.backend.domain.codef.exAccount.config.CodefExAccountProperties;
import com.team10.backend.domain.codef.exAccount.dto.internal.CodefExAccountConnectionPayload;
import com.team10.backend.domain.codef.exAccount.dto.internal.CodefExAccountConnectionResult;
import com.team10.backend.domain.codef.exAccount.dto.internal.CodefExAccountListRequest;
import com.team10.backend.domain.codef.exAccount.exception.CodefExAccountClientException;
import com.team10.backend.domain.codef.exAccount.exception.CodefExAccountRegistrationException;
import com.team10.backend.domain.codef.exAccount.exception.CodefExAccountRegistrationFailure;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import static com.team10.backend.domain.codef.exAccount.config.CodefExAccountRestClientConfig.API_REST_CLIENT;

@Component
public class CodefExAccountClient {

    private static final int MAX_ATTEMPTS = 2;

    private final CodefExAccountProperties properties;
    private final CodefExAccountAuthClient authClient;
    private final CodefExAccountResponseDecoder responseDecoder;
    private final RestClient restClient;

    public CodefExAccountClient(
            CodefExAccountProperties properties,
            CodefExAccountAuthClient authClient,
            CodefExAccountResponseDecoder responseDecoder,
            @Qualifier(API_REST_CLIENT) RestClient restClient
    ) {
        this.properties = properties;
        this.authClient = authClient;
        this.responseDecoder = responseDecoder;
        this.restClient = restClient;
    }

    public JsonNode getAccountList(CodefExAccountListRequest request) {
        validateRequest(request);

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                String responseBody = restClient.post()
                        .uri(properties.accountListPath())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authClient.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(String.class);
                return responseDecoder.decodeData(responseBody);
            } catch (RestClientResponseException exception) {
                if (!exception.getStatusCode().is5xxServerError() || attempt == MAX_ATTEMPTS) {
                    throw new CodefExAccountClientException("CODEF 보유계좌 HTTP 요청에 실패했습니다.", exception);
                }
            } catch (ResourceAccessException exception) {
                if (attempt == MAX_ATTEMPTS) {
                    throw new CodefExAccountClientException("CODEF 보유계좌 요청 시간이 초과되었습니다.", exception);
                }
            }
        }

        throw new CodefExAccountClientException("CODEF 보유계좌 조회에 실패했습니다.");
    }

    public CodefExAccountConnectionResult createConnection(
            CodefExAccountConnectionPayload payload
    ) {
        if (payload == null || payload.accountList().isEmpty()) {
            throw new CodefExAccountRegistrationException(
                    CodefExAccountRegistrationFailure.INVALID_RESPONSE,
                    "CODEF 계정등록 요청값이 올바르지 않습니다."
            );
        }

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                String responseBody = restClient.post()
                        .uri(properties.accountCreatePath())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authClient.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(payload)
                        .retrieve()
                        .body(String.class);
                return responseDecoder.decodeConnectionResult(responseBody);
            } catch (CodefExAccountRegistrationException exception) {
                throw exception;
            } catch (RestClientResponseException exception) {
                if (!exception.getStatusCode().is5xxServerError() || attempt == MAX_ATTEMPTS) {
                    throw registrationSystemError("CODEF 계정등록 HTTP 요청에 실패했습니다.", exception);
                }
            } catch (ResourceAccessException exception) {
                if (attempt == MAX_ATTEMPTS) {
                    throw registrationSystemError("CODEF 계정등록 요청 시간이 초과되었습니다.", exception);
                }
            } catch (CodefExAccountClientException exception) {
                throw new CodefExAccountRegistrationException(
                        CodefExAccountRegistrationFailure.INVALID_RESPONSE,
                        "CODEF 계정등록 응답이 올바르지 않습니다.",
                        exception
                );
            }
        }

        throw registrationSystemError("CODEF 계정등록에 실패했습니다.", null);
    }

    private void validateRequest(CodefExAccountListRequest request) {
        if (request == null
                || request.organization() == null
                || request.organization().isBlank()
                || request.connectedId() == null
                || request.connectedId().isBlank()) {
            throw new CodefExAccountClientException("CODEF 보유계좌 요청값이 올바르지 않습니다.");
        }
    }

    private CodefExAccountRegistrationException registrationSystemError(
            String message,
            Throwable cause
    ) {
        return new CodefExAccountRegistrationException(
                CodefExAccountRegistrationFailure.SYSTEM_ERROR,
                message,
                cause
        );
    }
}
