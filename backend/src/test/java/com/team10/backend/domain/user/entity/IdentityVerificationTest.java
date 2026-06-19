package com.team10.backend.domain.user.entity;

import com.team10.backend.domain.user.type.VerificationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityVerificationTest {

    private User newUser() {
        return User.create("test@example.com", "encoded-pw", "홍길동", "01012345678", LocalDate.of(1990, 1, 1));
    }

    @Nested
    @DisplayName("startOcr")
    class StartOcr {

        @Test
        @DisplayName("OCR_PENDING 상태로 생성된다")
        void createsWithOcrPendingStatus() {
            IdentityVerification verification = IdentityVerification.startOcr(newUser());

            assertThat(verification.getStatus()).isEqualTo(VerificationStatus.OCR_PENDING);
        }
    }

    @Nested
    @DisplayName("completeOcr")
    class CompleteOcr {

        @Test
        @DisplayName("OCR 결과가 기록되고 OCR_COMPLETED로 전환된다")
        void recordsResultAndTransitions() {
            IdentityVerification verification = IdentityVerification.startOcr(newUser());

            verification.completeOcr("홍길동", "901201-1234567", "2023-01-15");

            assertThat(verification.getOcrName()).isEqualTo("홍길동");
            assertThat(verification.getOcrResidentNumber()).isEqualTo("901201-1234567");
            assertThat(verification.getOcrIssueDate()).isEqualTo("2023-01-15");
            assertThat(verification.getStatus()).isEqualTo(VerificationStatus.OCR_COMPLETED);
        }
    }

    @Nested
    @DisplayName("completeGovernmentVerification")
    class CompleteGovernmentVerification {

        @Test
        @DisplayName("주민번호 뒷자리를 마스킹하고 GOVERNMENT_VERIFIED로 전환된다")
        void masksResidentNumberAndTransitions() {
            IdentityVerification verification = IdentityVerification.startOcr(newUser());
            verification.completeOcr("홍길동", "901201-1234567", "2023-01-15");

            verification.completeGovernmentVerification();

            assertThat(verification.getOcrResidentNumber()).isEqualTo("901201-*******");
            assertThat(verification.getStatus()).isEqualTo(VerificationStatus.GOVERNMENT_VERIFIED);
        }

        @Test
        @DisplayName("주민번호가 없으면 마스킹 없이 상태만 전환된다")
        void noResidentNumber_transitionsWithoutMasking() {
            IdentityVerification verification = IdentityVerification.startOcr(newUser());

            verification.completeGovernmentVerification();

            assertThat(verification.getOcrResidentNumber()).isNull();
            assertThat(verification.getStatus()).isEqualTo(VerificationStatus.GOVERNMENT_VERIFIED);
        }

        @Test
        @DisplayName("이미 마스킹된 주민번호는 재처리하지 않는다")
        void alreadyMasked_isIdempotent() {
            IdentityVerification verification = IdentityVerification.startOcr(newUser());
            verification.completeOcr("홍길동", "901201-1234567", "2023-01-15");
            verification.completeGovernmentVerification();
            String maskedOnce = verification.getOcrResidentNumber();

            verification.completeGovernmentVerification();

            assertThat(verification.getOcrResidentNumber()).isEqualTo(maskedOnce);
        }
    }

    @Nested
    @DisplayName("startOneWon / completeOneWon")
    class OneWonFlow {

        @Test
        @DisplayName("1원 송금 시작 시 ONE_WON_PENDING으로 전환된다")
        void startOneWon_transitions() {
            IdentityVerification verification = IdentityVerification.startOcr(newUser());

            verification.startOneWon();

            assertThat(verification.getStatus()).isEqualTo(VerificationStatus.ONE_WON_PENDING);
        }

        @Test
        @DisplayName("1원 송금 코드 검증 완료 시 COMPLETED로 전환된다")
        void completeOneWon_transitions() {
            IdentityVerification verification = IdentityVerification.startOcr(newUser());
            verification.startOneWon();

            verification.completeOneWon();

            assertThat(verification.getStatus()).isEqualTo(VerificationStatus.COMPLETED);
        }
    }

    @Nested
    @DisplayName("fail")
    class Fail {

        @Test
        @DisplayName("실패 사유를 기록하고 평문 주민번호가 있으면 마스킹한 뒤 FAILED로 전환된다")
        void recordsReasonAndMasksAndTransitions() {
            IdentityVerification verification = IdentityVerification.startOcr(newUser());
            verification.completeOcr("홍길동", "901201-1234567", "2023-01-15");

            verification.fail("행안부 연동 타임아웃: 잠시 후 다시 시도해주세요.");

            assertThat(verification.getFailureReason()).isEqualTo("행안부 연동 타임아웃: 잠시 후 다시 시도해주세요.");
            assertThat(verification.getOcrResidentNumber()).isEqualTo("901201-*******");
            assertThat(verification.getStatus()).isEqualTo(VerificationStatus.FAILED);
        }

        @Test
        @DisplayName("주민번호가 없는 상태에서 실패해도 예외 없이 FAILED로 전환된다")
        void noResidentNumber_stillTransitions() {
            IdentityVerification verification = IdentityVerification.startOcr(newUser());

            verification.fail("OCR 처리 실패");

            assertThat(verification.getFailureReason()).isEqualTo("OCR 처리 실패");
            assertThat(verification.getStatus()).isEqualTo(VerificationStatus.FAILED);
        }
    }
}
