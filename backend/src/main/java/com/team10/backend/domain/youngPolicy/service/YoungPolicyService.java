package com.team10.backend.domain.youngPolicy.service;

import com.team10.backend.domain.youngPolicy.client.YoungPolicyClient;
import com.team10.backend.domain.youngPolicy.dto.req.YoungPolicyReq;
import com.team10.backend.domain.youngPolicy.dto.res.YoungPolicyDetailRes;
import com.team10.backend.domain.youngPolicy.dto.res.YoungPolicyExternalRes;
import com.team10.backend.domain.youngPolicy.dto.res.YoungPolicySummaryRes;
import com.team10.backend.domain.youngPolicy.dto.res.YoungPolicySyncRes;
import com.team10.backend.domain.youngPolicy.exception.YoungPolicyErrorCode;
import com.team10.backend.domain.youngPolicy.repository.YoungPolicyRepository;
import com.team10.backend.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class YoungPolicyService {

    private final YoungPolicyRepository youngPolicyRepository;
    private final YoungPolicyClient youngPolicyClient;

    // 청년 정책 목록 조회
    public List<YoungPolicySummaryRes> getPolicies() {
        return youngPolicyRepository.findAll().stream()
                .map(YoungPolicySummaryRes::from)
                .toList();
    }

    // 청년 정책 상세 조회
    public YoungPolicyDetailRes getPolicy(Long id) {
        return youngPolicyRepository.findById(id)
                .map(YoungPolicyDetailRes::from)
                .orElseThrow(() -> new BusinessException(YoungPolicyErrorCode.YOUNG_POLICY_NOT_FOUND));
    }

    // 청년 정책 외부 API 연동 및 동기화
    @Transactional
    public YoungPolicySyncRes syncPolicies(YoungPolicyReq request) {
        YoungPolicyExternalRes response = fetchPolicies(request);
        int createdCount = 0;
        int updatedCount = 0;
        int skippedCount = 0;

        List<YoungPolicyExternalRes.PolicyItem> policyItems = response.policyItems();

        for (YoungPolicyExternalRes.PolicyItem item : policyItems) {
            if (!item.hasPolicyId()) {
                skippedCount++;
                continue;
            }

            var policy = youngPolicyRepository.findByPolicyId(item.plcyNo());
            if (policy.isPresent()) {
                item.update(policy.get());
                updatedCount++;
            } else {
                youngPolicyRepository.save(item.toEntity());
                createdCount++;
            }
        }

        return new YoungPolicySyncRes(
                policyItems.size(),
                createdCount,
                updatedCount,
                skippedCount
        );
    }

    // 청년 정책 외부 API 호출 및 응답 처리
    private YoungPolicyExternalRes fetchPolicies(YoungPolicyReq request) {
        try {
            YoungPolicyExternalRes response = youngPolicyClient.fetchPolicies(request);
            return response == null ? new YoungPolicyExternalRes(List.of()) : response;
        } catch (RuntimeException e) {
            throw new BusinessException(YoungPolicyErrorCode.YOUNG_POLICY_SYNC_FAILED, e);
        }
    }

}
