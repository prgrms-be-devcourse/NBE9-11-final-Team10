package com.team10.backend.domain.user.repository;

import com.team10.backend.domain.user.entity.IdentityVerification;
import com.team10.backend.domain.user.entity.User;
import com.team10.backend.global.config.QuerydslConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IdentityVerification.ocrResidentNumber에 적용된 CryptoStringConverter가
 * 실제 Hibernate 영속화 경로에서 동작하는지 확인하는 통합 테스트.
 *
 * <p>단위 테스트(CryptoStringConverterTest)는 컨버터 로직 자체만 검증하므로,
 * 여기서는 "엔티티를 저장하면 DB 컬럼에는 ciphertext가 들어가고,
 * 리포지토리로 다시 읽으면 평문으로 복호화되는지"를 EntityManager native query로 직접 확인한다.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(QuerydslConfig.class)
class IdentityVerificationEncryptionTest {

    @Autowired
    private IdentityVerificationRepository identityVerificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User persistUser(String email) {
        return userRepository.save(
                User.create(email, "encoded-pw", "홍길동", "01012345678", LocalDate.of(1990, 1, 1)));
    }

    private String rawResidentNumber(Long verificationId) {
        return (String) entityManager
                .createNativeQuery("SELECT ocr_resident_number FROM identity_verifications WHERE id = :id")
                .setParameter("id", verificationId)
                .getSingleResult();
    }

    @Test
    @DisplayName("DB에는 암호화되어 저장되고, 리포지토리로 조회하면 평문으로 복호화된다")
    void ocrResidentNumber_isEncryptedAtRest_andDecryptedOnRead() {
        User user = persistUser("encrypt-test@example.com");

        IdentityVerification verification = IdentityVerification.startOcr(user);
        verification.completeOcr("홍길동", "901201-1234567", "2023-01-15");
        Long id = identityVerificationRepository.save(verification).getId();

        entityManager.flush();
        entityManager.clear();

        String rawValue = rawResidentNumber(id);
        assertThat(rawValue).isNotEqualTo("901201-1234567");
        assertThat(rawValue).doesNotContain("901201");

        IdentityVerification reloaded = identityVerificationRepository.findById(id).orElseThrow();
        assertThat(reloaded.getOcrResidentNumber()).isEqualTo("901201-1234567");
    }

    @Test
    @DisplayName("같은 평문을 두 번 저장해도 raw 컬럼 값은 서로 다르다 (랜덤 IV)")
    void samePlainText_producesDifferentRawValues() {
        User user1 = persistUser("encrypt-test-1@example.com");
        User user2 = persistUser("encrypt-test-2@example.com");

        IdentityVerification v1 = IdentityVerification.startOcr(user1);
        v1.completeOcr("홍길동", "901201-1234567", "2023-01-15");
        Long id1 = identityVerificationRepository.save(v1).getId();

        IdentityVerification v2 = IdentityVerification.startOcr(user2);
        v2.completeOcr("홍길동", "901201-1234567", "2023-01-15");
        Long id2 = identityVerificationRepository.save(v2).getId();

        entityManager.flush();
        entityManager.clear();

        assertThat(rawResidentNumber(id1)).isNotEqualTo(rawResidentNumber(id2));
    }

    @Test
    @DisplayName("마스킹 후 저장된 값도 암호화되어 저장되고 복호화 시 마스킹된 형태로 돌아온다")
    void maskedResidentNumber_isAlsoEncrypted() {
        User user = persistUser("encrypt-test-masked@example.com");

        IdentityVerification verification = IdentityVerification.startOcr(user);
        verification.completeOcr("홍길동", "901201-1234567", "2023-01-15");
        verification.completeGovernmentVerification();
        Long id = identityVerificationRepository.save(verification).getId();

        entityManager.flush();
        entityManager.clear();

        String rawValue = rawResidentNumber(id);
        assertThat(rawValue).doesNotContain("901201-*******");

        IdentityVerification reloaded = identityVerificationRepository.findById(id).orElseThrow();
        assertThat(reloaded.getOcrResidentNumber()).isEqualTo("901201-*******");
    }
}
