package com.team10.backend.domain.codef.exAccount.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team10.backend.domain.codef.exAccount.exception.CodefExAccountClientException;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Component
public class CodefExAccountResponseDecoder {

    private static final String SUCCESS_CODE = "CF-00000";

    private final ObjectMapper objectMapper;

    public CodefExAccountResponseDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode decodeData(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new CodefExAccountClientException("CODEF 보유계좌 응답이 비어 있습니다.");
        }

        try {
            String decodedBody = URLDecoder.decode(responseBody, StandardCharsets.UTF_8);
            JsonNode root = objectMapper.readTree(decodedBody);
            String resultCode = root.path("result").path("code").asText();
            JsonNode data = root.path("data");
            if (!SUCCESS_CODE.equals(resultCode) || data.isMissingNode() || data.isNull()) {
                throw new CodefExAccountClientException("CODEF 보유계좌 조회에 실패했습니다.");
            }
            return data;
        } catch (IllegalArgumentException | JsonProcessingException exception) {
            throw new CodefExAccountClientException("CODEF 보유계좌 응답을 해석할 수 없습니다.", exception);
        }
    }
}
