package com.team10.backend.domain.codef.exAccount.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team10.backend.domain.codef.exAccount.exception.CodefExAccountClientException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CodefExAccountResponseDecoderTest {

    private final CodefExAccountResponseDecoder decoder =
            new CodefExAccountResponseDecoder(new ObjectMapper());

    @Test
    void rejectsCodefFailureWithoutExposingUpstreamMessage() {
        String response = """
                {
                  "result": {
                    "code": "CF-94002",
                    "message": "민감한 기관 원문 메시지"
                  },
                  "data": {}
                }
                """;

        assertThatThrownBy(() -> decoder.decodeData(response))
                .isInstanceOf(CodefExAccountClientException.class)
                .hasMessage("CODEF 보유계좌 조회에 실패했습니다.")
                .hasMessageNotContaining("민감한 기관 원문 메시지");
    }

    @Test
    void rejectsMalformedResponse() {
        assertThatThrownBy(() -> decoder.decodeData("not-json"))
                .isInstanceOf(CodefExAccountClientException.class)
                .hasMessage("CODEF 보유계좌 응답을 해석할 수 없습니다.");
    }
}
