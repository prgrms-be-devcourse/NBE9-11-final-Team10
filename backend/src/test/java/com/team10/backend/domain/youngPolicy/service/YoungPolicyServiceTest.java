package com.team10.backend.domain.youngPolicy.service;

import com.team10.backend.domain.youngPolicy.client.YoungPolicyClient;
import com.team10.backend.domain.youngPolicy.dto.req.YoungPolicyReq;
import com.team10.backend.domain.youngPolicy.dto.res.YoungPolicyDetailRes;
import com.team10.backend.domain.youngPolicy.dto.res.YoungPolicyExternalRes;
import com.team10.backend.domain.youngPolicy.dto.res.YoungPolicySummaryRes;
import com.team10.backend.domain.youngPolicy.dto.res.YoungPolicySyncRes;
import com.team10.backend.domain.youngPolicy.entity.YoungPolicy;
import com.team10.backend.domain.youngPolicy.exception.YoungPolicyErrorCode;
import com.team10.backend.domain.youngPolicy.repository.YoungPolicyRepository;
import com.team10.backend.domain.youngPolicy.repository.YoungPolicyRepositoryTest;
import com.team10.backend.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class YoungPolicyServiceTest {

    @Mock
    private YoungPolicyRepository youngPolicyRepository;

    @Mock
    private YoungPolicyClient youngPolicyClient;

    @InjectMocks
    private YoungPolicyService youngPolicyService;

    @Test
    @DisplayName("mock 청년 정책 목록을 요약 응답으로 조회한다")
    void getPolicies_returnsSummaryResponses() {
        YoungPolicy policy = YoungPolicyRepositoryTest.createPolicy();
        when(youngPolicyRepository.findAll()).thenReturn(List.of(policy));

        List<YoungPolicySummaryRes> responses = youngPolicyService.getPolicies();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(YoungPolicyRepositoryTest.POLICY_ID);
        assertThat(responses.get(0).policyId()).isEqualTo(YoungPolicyRepositoryTest.EXTERNAL_POLICY_ID);
        assertThat(responses.get(0).title()).isEqualTo(YoungPolicyRepositoryTest.POLICY_TITLE);
        assertThat(responses.get(0).category()).isEqualTo(YoungPolicyRepositoryTest.POLICY_CATEGORY);
    }

    @Test
    @DisplayName("mock 청년 정책 상세를 조회한다")
    void getPolicy_returnsDetailResponse() {
        YoungPolicy policy = YoungPolicyRepositoryTest.createPolicy();
        when(youngPolicyRepository.findById(YoungPolicyRepositoryTest.POLICY_ID)).thenReturn(Optional.of(policy));

        YoungPolicyDetailRes response = youngPolicyService.getPolicy(YoungPolicyRepositoryTest.POLICY_ID);

        assertThat(response.id()).isEqualTo(YoungPolicyRepositoryTest.POLICY_ID);
        assertThat(response.policyId()).isEqualTo(YoungPolicyRepositoryTest.EXTERNAL_POLICY_ID);
        assertThat(response.title()).isEqualTo(YoungPolicyRepositoryTest.POLICY_TITLE);
        assertThat(response.description()).isEqualTo(YoungPolicyRepositoryTest.POLICY_DESCRIPTION);
        assertThat(response.applyUrl()).isEqualTo(YoungPolicyRepositoryTest.POLICY_APPLY_URL);
    }

    @Test
    @DisplayName("mock 청년 정책이 없으면 상세 조회에 실패한다")
    void getPolicy_notFound_throwsBusinessException() {
        when(youngPolicyRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> youngPolicyService.getPolicy(999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(YoungPolicyErrorCode.YOUNG_POLICY_NOT_FOUND);
    }

    @Test
    @DisplayName("현재 청년정책 API 응답의 result.plcyList를 DB에 저장한다")
    void syncPolicies_savesPoliciesFromCurrentApiResult() {
        YoungPolicyReq request = new YoungPolicyReq(1, 10);
        YoungPolicyExternalRes.PolicyItem item = new YoungPolicyExternalRes.PolicyItem(
                "20260305005400112100",
                "청년문화예술패스",
                "청년의 문화향유 기회를 제공",
                null,
                "금융/복지/문화",
                "문화",
                19,
                20,
                null,
                "서울특별시",
                "001",
                null,
                "20260225 ~ 20260630",
                "https://example.com",
                "온라인 신청"
        );
        YoungPolicyExternalRes response = new YoungPolicyExternalRes(
                List.of(),
                new YoungPolicyExternalRes.Result(List.of(item))
        );
        when(youngPolicyClient.fetchPolicies(request)).thenReturn(response);
        when(youngPolicyRepository.findByPolicyId("20260305005400112100")).thenReturn(Optional.empty());

        YoungPolicySyncRes syncResponse = youngPolicyService.syncPolicies(request);

        assertThat(syncResponse.fetchedCount()).isEqualTo(1);
        assertThat(syncResponse.createdCount()).isEqualTo(1);
        assertThat(syncResponse.updatedCount()).isZero();
        assertThat(syncResponse.skippedCount()).isZero();
        verify(youngPolicyRepository).save(any(YoungPolicy.class));
    }
}
