package com.team10.backend.global.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 서버 비밀키를 사용해 입력값의 HMAC-SHA-256 해시를 생성한다.
 * 동일한 키와 입력값은 항상 동일한 결과를 반환하므로 blind index처럼
 * 원문을 저장하지 않고 동일값을 조회해야 하는 경우에 사용한다.
 */
@Component
public class HmacSha256Hasher {

    private static final String ALGORITHM = "HmacSHA256";

    /** 애플리케이션 시작 시 Base64 설정값을 디코딩해 생성한 HMAC 비밀키다. */
    private final SecretKeySpec secretKey;

    /**
     * 설정된 Base64 형식의 비밀키를 HMAC 키로 변환한다.
     * 실제 키는 소스에 저장하지 않고 환경변수나 Secret Manager로 주입해야 한다.
     */
    public HmacSha256Hasher(
            @Value("${security.hmac.secret}") String base64EncodedSecret
    ) {
        this.secretKey = createSecretKey(base64EncodedSecret);
    }

    /**
     * 입력값을 UTF-8 바이트로 변환해 HMAC-SHA-256을 계산하고 64자리 Hex 문자열로 반환한다.
     * Mac은 스레드 안전하지 않으므로 호출할 때마다 새로운 인스턴스를 생성한다.
     */
    public String hash(String value) {
        Objects.requireNonNull(value, "해싱할 값은 null일 수 없습니다.");

        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(secretKey);

            byte[] hash = mac.doFinal(
                    value.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "HMAC-SHA-256 해시 생성에 실패했습니다.",
                    exception
            );
        }
    }

    /** Base64 비밀키를 디코딩하고 HMAC-SHA-256에 사용할 키 객체를 생성한다. */
    private SecretKeySpec createSecretKey(String base64EncodedSecret) {
        if (base64EncodedSecret == null || base64EncodedSecret.isBlank()) {
            throw new IllegalArgumentException(
                    "HMAC 비밀키가 설정되지 않았습니다."
            );
        }

        try {
            byte[] decodedSecret = Base64.getDecoder()
                    .decode(base64EncodedSecret);

            if (decodedSecret.length == 0) {
                throw new IllegalArgumentException(
                        "HMAC 비밀키는 비어 있을 수 없습니다."
                );
            }

            return new SecretKeySpec(decodedSecret, ALGORITHM);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "HMAC 비밀키는 유효한 Base64 형식이어야 합니다.",
                    exception
            );
        }
    }
}
