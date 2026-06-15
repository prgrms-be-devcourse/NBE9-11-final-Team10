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
import com.team10.backend.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
        SyncCount count = new SyncCount();

        for (YoungPolicyExternalRes.PolicyItem item : response.youthPolicyList()) {
            if (!StringUtils.hasText(item.plcyNo())) {
                count.skipped++;
                continue;
            }

            youngPolicyRepository.findByPolicyId(item.plcyNo())
                    .ifPresentOrElse(
                            policy -> {
                                updatePolicy(policy, item);
                                count.updated++;
                            },
                            () -> {
                                youngPolicyRepository.save(createPolicy(item));
                                count.created++;
                            }
                    );
        }

        return new YoungPolicySyncRes(
                response.youthPolicyList().size(),
                count.created,
                count.updated,
                count.skipped
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

    // 청년 정책 엔티티 생성 및 업데이트 메서드
    private YoungPolicy createPolicy(YoungPolicyExternalRes.PolicyItem item) {
        return new YoungPolicy(
                item.plcyNo(),
                item.plcyNm(),
                item.plcyExplnCn(),
                item.lclsfNm(),
                item.mclsfNm(),
                parseAge(item.sprtTrgtMinAge()),
                parseAge(item.sprtTrgtMaxAge()),
                item.zipCd(),
                item.jobCd(),
                item.aplyYmd(),
                item.aplyUrlAddr(),
                item.plcyAplyMthdCn()
        );
    }

    // 기존 정책 업데이트 시 모든 필드를 새 값으로 덮어쓰는 형태로 업데이트
    private void updatePolicy(YoungPolicy policy, YoungPolicyExternalRes.PolicyItem item) {
        policy.updateFrom(
                item.plcyNm(),
                item.plcyExplnCn(),
                item.lclsfNm(),
                item.mclsfNm(),
                parseAge(item.sprtTrgtMinAge()),
                parseAge(item.sprtTrgtMaxAge()),
                item.zipCd(),
                item.jobCd(),
                item.aplyYmd(),
                item.aplyUrlAddr(),
                item.plcyAplyMthdCn()
        );
    }

    // 청년 정책의 나이 필드는 문자열로 제공되므로, 정수로 변환하는 유틸리티 메서드 추가
    private Integer parseAge(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // 청년 정책 동기화 결과를 집계하기 위한 내부 클래스 추가
    private static class SyncCount {
        private int created;
        private int updated;
        private int skipped;
    }
}
