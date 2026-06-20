package com.team10.backend.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.Arrays;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HmacSha256HasherTest {

    @Test
    @DisplayName("RFC 4231 HMAC-SHA-256 테스트 벡터와 같은 결과를 생성한다")
    void hashMatchesKnownVector() {
        byte[] secret = new byte[20];
        Arrays.fill(secret, (byte) 0x0b);
        HmacSha256Hasher hasher = new HmacSha256Hasher(
                Base64.getEncoder().encodeToString(secret)
        );

        String result = hasher.hash("Hi There");

        assertThat(result).isEqualTo(
                "b0344c61d8db38535ca8afceaf0bf12b"
                        + "881dc200c9833da726e9376c2e32cff7"
        );
    }

    @Test
    @DisplayName("같은 키와 같은 입력은 항상 같은 해시를 생성한다")
    void hashIsDeterministic() {
        HmacSha256Hasher hasher = new HmacSha256Hasher("dGVzdC1obWFjLXNlY3JldA==");

        assertThat(hasher.hash("12345678901234"))
                .isEqualTo(hasher.hash("12345678901234"));
    }

    @Test
    @DisplayName("null은 해싱할 수 없다")
    void hashRejectsNull() {
        HmacSha256Hasher hasher = new HmacSha256Hasher("dGVzdC1obWFjLXNlY3JldA==");

        assertThatNullPointerException()
                .isThrownBy(() -> hasher.hash(null))
                .withMessage("해싱할 값은 null일 수 없습니다.");
    }
}
