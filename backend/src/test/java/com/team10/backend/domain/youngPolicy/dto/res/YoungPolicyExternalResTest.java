package com.team10.backend.domain.youngPolicy.dto.res;

import com.team10.backend.domain.youngPolicy.entity.YoungPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class YoungPolicyExternalResTest {

    @Test
    @DisplayName("연령 값이 숫자 또는 소수 문자열이면 정수 연령으로 변환한다")
    void policyItemToEntityParsesNumericAgeValues() {
        YoungPolicyExternalRes.PolicyItem item = new YoungPolicyExternalRes.PolicyItem(
                "20260305005400112100",
                "청년문화예술패스",
                "청년의 문화향유 기회를 제공",
                "금융/복지/문화",
                null,
                "문화",
                19.0,
                "20.0",
                "11000",
                null,
                "001",
                "20260225 ~ 20260630",
                null,
                "https://example.com",
                "온라인 신청"
        );

        YoungPolicy policy = item.toEntity();

        assertThat(policy.getMinAge()).isEqualTo(19);
        assertThat(policy.getMaxAge()).isEqualTo(20);
    }
}
