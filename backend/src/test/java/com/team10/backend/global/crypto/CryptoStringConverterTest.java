package com.team10.backend.global.crypto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CryptoStringConverterTest {

    private static final String TEST_KEY = "IHVNIRQkRQVL467W70YUxpqSszxvhquhFmIuxCkd2Us=";

    private CryptoStringConverter newConverter() {
        return new CryptoStringConverter(TEST_KEY);
    }

    @Nested
    @DisplayName("생성자")
    class Constructor {

        @Test
        @DisplayName("32바이트가 아닌 Base64 키면 예외가 발생한다")
        void invalidKeyLength_throws() {
            String shortKey = Base64.getEncoder().encodeToString(new byte[16]);

            assertThatThrownBy(() -> new CryptoStringConverter(shortKey))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("Base64 형식이 아니면 예외가 발생한다")
        void notBase64_throws() {
            assertThatThrownBy(() -> new CryptoStringConverter("not-base64!!"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("암복호화 라운드트립")
    class RoundTrip {

        @Test
        @DisplayName("암호화 후 복호화하면 원래 평문을 얻는다")
        void encryptThenDecrypt_returnsOriginal() {
            CryptoStringConverter converter = newConverter();
            String plainText = "901201-1234567";

            String encrypted = converter.convertToDatabaseColumn(plainText);
            String decrypted = converter.convertToEntityAttribute(encrypted);

            assertThat(decrypted).isEqualTo(plainText);
        }

        @Test
        @DisplayName("암호화된 값은 평문과 다르고 평문을 포함하지 않는다")
        void encryptedValue_doesNotContainPlainText() {
            CryptoStringConverter converter = newConverter();
            String plainText = "901201-1234567";

            String encrypted = converter.convertToDatabaseColumn(plainText);

            assertThat(encrypted).isNotEqualTo(plainText);
            assertThat(encrypted).doesNotContain(plainText);
        }

        @Test
        @DisplayName("같은 평문을 두 번 암호화하면 매번 다른 값이 나온다 (랜덤 IV)")
        void samePlainText_producesDifferentCiphertextEachTime() {
            CryptoStringConverter converter = newConverter();
            String plainText = "901201-1234567";

            String first = converter.convertToDatabaseColumn(plainText);
            String second = converter.convertToDatabaseColumn(plainText);

            assertThat(first).isNotEqualTo(second);
            // 다만 복호화하면 둘 다 같은 평문으로 돌아온다.
            assertThat(converter.convertToEntityAttribute(first)).isEqualTo(plainText);
            assertThat(converter.convertToEntityAttribute(second)).isEqualTo(plainText);
        }

        @Test
        @DisplayName("null을 암호화하면 null을 반환한다")
        void encryptNull_returnsNull() {
            CryptoStringConverter converter = newConverter();

            assertThat(converter.convertToDatabaseColumn(null)).isNull();
        }

        @Test
        @DisplayName("null을 복호화하면 null을 반환한다")
        void decryptNull_returnsNull() {
            CryptoStringConverter converter = newConverter();

            assertThat(converter.convertToEntityAttribute(null)).isNull();
        }
    }

    @Nested
    @DisplayName("변조/오류 데이터")
    class TamperedData {

        @Test
        @DisplayName("변조된 ciphertext를 복호화하면 예외가 발생한다 (GCM 인증 태그 검증 실패)")
        void tamperedCiphertext_throws() {
            CryptoStringConverter converter = newConverter();
            String encrypted = converter.convertToDatabaseColumn("901201-1234567");

            byte[] combined = Base64.getDecoder().decode(encrypted);
            combined[combined.length - 1] ^= 0x01; // 마지막 바이트 변조
            String tampered = Base64.getEncoder().encodeToString(combined);

            assertThatThrownBy(() -> converter.convertToEntityAttribute(tampered))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("다른 키로 암호화된 값은 복호화할 수 없다")
        void differentKey_cannotDecrypt() {
            CryptoStringConverter converterA = newConverter();
            CryptoStringConverter converterB = new CryptoStringConverter(
                    "VLcru3YL6wQ99OOucGdQp6l+uLzD369+zylscNgTNJc=");

            String encryptedByA = converterA.convertToDatabaseColumn("901201-1234567");

            assertThatThrownBy(() -> converterB.convertToEntityAttribute(encryptedByA))
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}
