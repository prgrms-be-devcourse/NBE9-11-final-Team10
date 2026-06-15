package com.team10.backend.domain.youngPolicy.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.team10.backend.domain.youngPolicy.dto.res.YoungPolicyDetailRes;
import com.team10.backend.domain.youngPolicy.dto.res.YoungPolicySummaryRes;
import com.team10.backend.domain.youngPolicy.entity.YoungPolicy;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class YoungPolicyRepositoryTest {

    public static final Long POLICY_ID = 1L;
    public static final String EXTERNAL_POLICY_ID = "YP-001";
    public static final String POLICY_TITLE = "청년 월세 지원";
    public static final String POLICY_DESCRIPTION = "청년 월세 지원 설명";
    public static final String POLICY_CATEGORY = "주거";
    public static final String POLICY_SUB_CATEGORY = "주거비";
    public static final Integer POLICY_MIN_AGE = 19;
    public static final Integer POLICY_MAX_AGE = 34;
    public static final String POLICY_REGION_CODE = "11";
    public static final String POLICY_JOB_CODE = "001";
    public static final String POLICY_APPLY_PERIOD = "20260601~20260630";
    public static final String POLICY_APPLY_URL = "https://example.com/policies/YP-001";
    public static final String POLICY_APPLY_METHOD = "온라인 신청";

    @Test
    @DisplayName("service와 controller 테스트에서 사용할 mock 청년 정책 엔티티를 생성한다")
    void createPolicy_returnsMockPolicyEntity() {
        YoungPolicy policy = createPolicy();

        assertThat(policy.getId()).isEqualTo(POLICY_ID);
        assertThat(policy.getPolicyId()).isEqualTo(EXTERNAL_POLICY_ID);
        assertThat(policy.getTitle()).isEqualTo(POLICY_TITLE);
        assertThat(policy.getDescription()).isEqualTo(POLICY_DESCRIPTION);
        assertThat(policy.getCategory()).isEqualTo(POLICY_CATEGORY);
        assertThat(policy.getSubCategory()).isEqualTo(POLICY_SUB_CATEGORY);
        assertThat(policy.getMinAge()).isEqualTo(POLICY_MIN_AGE);
        assertThat(policy.getMaxAge()).isEqualTo(POLICY_MAX_AGE);
        assertThat(policy.getRegionCode()).isEqualTo(POLICY_REGION_CODE);
        assertThat(policy.getJobCode()).isEqualTo(POLICY_JOB_CODE);
        assertThat(policy.getApplyPeriod()).isEqualTo(POLICY_APPLY_PERIOD);
        assertThat(policy.getApplyUrl()).isEqualTo(POLICY_APPLY_URL);
        assertThat(policy.getApplyMethod()).isEqualTo(POLICY_APPLY_METHOD);
    }

    @Test
    @DisplayName("controller 테스트에서 사용할 mock 청년 정책 요약 응답을 생성한다")
    void createSummaryResponse_returnsMockSummaryResponse() {
        YoungPolicySummaryRes response = createSummaryResponse();

        assertThat(response.id()).isEqualTo(POLICY_ID);
        assertThat(response.policyId()).isEqualTo(EXTERNAL_POLICY_ID);
        assertThat(response.title()).isEqualTo(POLICY_TITLE);
        assertThat(response.category()).isEqualTo(POLICY_CATEGORY);
        assertThat(response.subCategory()).isEqualTo(POLICY_SUB_CATEGORY);
        assertThat(response.minAge()).isEqualTo(POLICY_MIN_AGE);
        assertThat(response.maxAge()).isEqualTo(POLICY_MAX_AGE);
        assertThat(response.regionCode()).isEqualTo(POLICY_REGION_CODE);
        assertThat(response.applyPeriod()).isEqualTo(POLICY_APPLY_PERIOD);
    }

    @Test
    @DisplayName("controller 테스트에서 사용할 mock 청년 정책 상세 응답을 생성한다")
    void createDetailResponse_returnsMockDetailResponse() {
        YoungPolicyDetailRes response = createDetailResponse();

        assertThat(response.id()).isEqualTo(POLICY_ID);
        assertThat(response.policyId()).isEqualTo(EXTERNAL_POLICY_ID);
        assertThat(response.title()).isEqualTo(POLICY_TITLE);
        assertThat(response.description()).isEqualTo(POLICY_DESCRIPTION);
        assertThat(response.applyUrl()).isEqualTo(POLICY_APPLY_URL);
        assertThat(response.applyMethod()).isEqualTo(POLICY_APPLY_METHOD);
    }

    public static List<YoungPolicy> createPolicies() {
        return List.of(createPolicy());
    }

    public static YoungPolicy createPolicy() {
        YoungPolicy policy = new YoungPolicy(
                EXTERNAL_POLICY_ID,
                POLICY_TITLE,
                POLICY_DESCRIPTION,
                POLICY_CATEGORY,
                POLICY_SUB_CATEGORY,
                POLICY_MIN_AGE,
                POLICY_MAX_AGE,
                POLICY_REGION_CODE,
                POLICY_JOB_CODE,
                POLICY_APPLY_PERIOD,
                POLICY_APPLY_URL,
                POLICY_APPLY_METHOD
        );
        ReflectionTestUtils.setField(policy, "id", POLICY_ID);
        return policy;
    }

    public static YoungPolicySummaryRes createSummaryResponse() {
        return new YoungPolicySummaryRes(
                POLICY_ID,
                EXTERNAL_POLICY_ID,
                POLICY_TITLE,
                POLICY_CATEGORY,
                POLICY_SUB_CATEGORY,
                POLICY_MIN_AGE,
                POLICY_MAX_AGE,
                POLICY_REGION_CODE,
                POLICY_APPLY_PERIOD
        );
    }

    public static YoungPolicyDetailRes createDetailResponse() {
        return new YoungPolicyDetailRes(
                POLICY_ID,
                EXTERNAL_POLICY_ID,
                POLICY_TITLE,
                POLICY_DESCRIPTION,
                POLICY_CATEGORY,
                POLICY_SUB_CATEGORY,
                POLICY_MIN_AGE,
                POLICY_MAX_AGE,
                POLICY_REGION_CODE,
                POLICY_JOB_CODE,
                POLICY_APPLY_PERIOD,
                POLICY_APPLY_URL,
                POLICY_APPLY_METHOD
        );
    }
}
