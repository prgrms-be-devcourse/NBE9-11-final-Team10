package com.team10.backend.domain.youngPolicy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.team10.backend.domain.youngPolicy.dto.res.YoungPolicyDetailRes;
import com.team10.backend.domain.youngPolicy.dto.res.YoungPolicySummaryRes;
import com.team10.backend.domain.youngPolicy.exception.YoungPolicyErrorCode;
import com.team10.backend.domain.youngPolicy.repository.YoungPolicyMockData;
import com.team10.backend.domain.youngPolicy.repository.YoungPolicyMockRepository;
import com.team10.backend.global.exception.BusinessException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class YoungPolicyServiceTest {

    @Mock
    private YoungPolicyMockRepository youngPolicyMockRepository;

    @InjectMocks
    private YoungPolicyService youngPolicyService;

    @Test
    @DisplayName("mock 청년 정책 목록을 요약 응답으로 조회한다")
    void getPolicies_returnsSummaryResponses() {
        YoungPolicyMockData policy = createPolicy(1L, "YP-001", "청년 월세 지원");
        when(youngPolicyMockRepository.findAll()).thenReturn(List.of(policy));

        List<YoungPolicySummaryRes> responses = youngPolicyService.getPolicies();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(1L);
        assertThat(responses.get(0).policyId()).isEqualTo("YP-001");
        assertThat(responses.get(0).title()).isEqualTo("청년 월세 지원");
        assertThat(responses.get(0).category()).isEqualTo("주거");
    }

    @Test
    @DisplayName("mock 청년 정책 상세를 조회한다")
    void getPolicy_returnsDetailResponse() {
        YoungPolicyMockData policy = createPolicy(1L, "YP-001", "청년 월세 지원");
        when(youngPolicyMockRepository.findById(1L)).thenReturn(Optional.of(policy));

        YoungPolicyDetailRes response = youngPolicyService.getPolicy(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.policyId()).isEqualTo("YP-001");
        assertThat(response.title()).isEqualTo("청년 월세 지원");
        assertThat(response.description()).isEqualTo("청년 월세 지원 설명");
        assertThat(response.applyUrl()).isEqualTo("https://example.com/policies/YP-001");
    }

    @Test
    @DisplayName("mock 청년 정책이 없으면 상세 조회에 실패한다")
    void getPolicy_notFound_throwsBusinessException() {
        when(youngPolicyMockRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> youngPolicyService.getPolicy(999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(YoungPolicyErrorCode.YOUNG_POLICY_NOT_FOUND);
    }

    private YoungPolicyMockData createPolicy(Long id, String policyId, String title) {
        return new YoungPolicyMockData(
                id,
                policyId,
                title,
                title + " 설명",
                "주거",
                "주거비",
                19,
                34,
                "11",
                "001",
                "20260601~20260630",
                "https://example.com/policies/" + policyId,
                "온라인 신청"
        );
    }
}
