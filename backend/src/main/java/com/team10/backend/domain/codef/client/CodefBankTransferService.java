package com.team10.backend.domain.codef.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team10.backend.domain.user.exception.UserErrorCode;
import com.team10.backend.domain.user.verification.BankTransferService;
import com.team10.backend.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** CODEF API 기반 1원 계좌인증 송금 서비스. */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class CodefBankTransferService implements BankTransferService {

    // 운영 전환 시 development.codef.io → api 도메인으로 교체
    private static final String TRANSFER_AUTH_URL =
            "https://development.codef.io/v1/kr/bank/a/account/transfer-authentication";
    // inPrintType=9: 고객사 직접 입력 — inPrintContent에 지정한 코드를 입금자명으로 사용
    private static final String IN_PRINT_TYPE_CUSTOM = "9";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final CodefAuthClient codefAuthClient;
    private final RestClient codefBankTransferRestClient;

    @Override
    public void sendOneWon(String organization, String accountNumber, String verificationCode) {
        Map<?, ?> responseMap = requestTransfer(organization, accountNumber, verificationCode);
        Map<?, ?> result = (responseMap != null) ? (Map<?, ?>) responseMap.get("result") : null;
        String code = (result != null) ? (String) result.get("code") : null;

        if (!"CF-00000".equals(code)) {
            log.error("[CODEF] 1원 송금 실패 — org={}, account={}, code={}, message={}",
                    organization, accountNumber, code, result != null ? result.get("message") : null);
            throw new BusinessException(UserErrorCode.ONE_WON_TRANSFER_FAILED);
        }

        log.info("[CODEF] 1원 송금 완료 — org={}, account={}, code={}", organization, accountNumber, verificationCode);
    }

    /** 1원 송금 인증 API 호출 + 응답 디코딩. 실패 시 전부 ONE_WON_TRANSFER_FAILED로 변환한다. */
    private Map<?, ?> requestTransfer(String organization, String accountNumber, String verificationCode) {
        Map<String, Object> body = new HashMap<>();
        body.put("organization", organization);
        body.put("account", accountNumber);
        body.put("inPrintType", IN_PRINT_TYPE_CUSTOM);
        body.put("inPrintContent", verificationCode);

        try {
            String token = codefAuthClient.getAccessToken();

            String response = codefBankTransferRestClient.post()
                    .uri(TRANSFER_AUTH_URL)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            String decoded = URLDecoder.decode(response != null ? response : "", StandardCharsets.UTF_8);
            return OBJECT_MAPPER.readValue(decoded, Map.class);

        } catch (CodefAuthException | RestClientException | IllegalArgumentException | JsonProcessingException e) {
            log.error("[CODEF] 1원 송금 중 오류 — org={}, account={}", organization, accountNumber, e);
            throw new BusinessException(UserErrorCode.ONE_WON_TRANSFER_FAILED);
        }
    }
}
