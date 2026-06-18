package com.team10.backend.global.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM 기반 String 컬럼 암복호화 컨버터.
 *
 * <p>저장 형식: {@code Base64( IV(12byte) || ciphertext || authTag(16byte) )}.
 * GCM은 매 암호화마다 랜덤 IV를 사용하므로 동일한 평문이라도 호출마다 다른 ciphertext가 생성된다.
 * 따라서 이 컨버터가 적용된 컬럼은 동등(=) 검색이 불가능하다 — 적용 대상은 평문 검색이 필요 없는
 * 민감정보(예: 주민등록번호)로 한정한다.
 *
 * <p>{@code @Component}로 등록해 Spring Boot의 Hibernate 빈 컨테이너 연동을 통해 생성자에
 * {@code app.encryption.key}를 주입받는다. {@code autoApply = false}이므로 적용할 필드에
 * {@code @Convert(converter = CryptoStringConverter.class)}를 명시해야 한다.
 */
@Component
@Converter(autoApply = false)
public class CryptoStringConverter implements AttributeConverter<String, String> {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKeySpec keySpec;
    private final SecureRandom secureRandom = new SecureRandom();

    public CryptoStringConverter(@Value("${app.encryption.key}") String base64Key) {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(base64Key);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("app.encryption.key는 유효한 Base64 문자열이어야 합니다.", e);
        }
        if (keyBytes.length != 32) {
            throw new IllegalStateException(
                    "app.encryption.key는 Base64로 인코딩된 32바이트(AES-256) 키여야 합니다. 현재 길이: " + keyBytes.length + "byte");
        }
        this.keySpec = new SecretKeySpec(keyBytes, "AES");
    }

    @Override
    public String convertToDatabaseColumn(String plainText) {
        if (plainText == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[IV_LENGTH_BYTES + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH_BYTES);
            System.arraycopy(cipherText, 0, combined, IV_LENGTH_BYTES, cipherText.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("값 암호화에 실패했습니다.", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String storedValue) {
        if (storedValue == null) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(storedValue);
            if (combined.length <= IV_LENGTH_BYTES) {
                throw new IllegalStateException("저장된 암호화 값의 길이가 올바르지 않습니다.");
            }

            byte[] iv = new byte[IV_LENGTH_BYTES];
            byte[] cipherText = new byte[combined.length - IV_LENGTH_BYTES];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH_BYTES);
            System.arraycopy(combined, IV_LENGTH_BYTES, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] plainBytes = cipher.doFinal(cipherText);

            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            // 키가 바뀌었거나 데이터가 변조된 경우 GCM 인증 태그 검증에서 실패한다.
            throw new IllegalStateException("값 복호화에 실패했습니다. 키가 변경되었거나 데이터가 손상되었을 수 있습니다.", e);
        }
    }
}
