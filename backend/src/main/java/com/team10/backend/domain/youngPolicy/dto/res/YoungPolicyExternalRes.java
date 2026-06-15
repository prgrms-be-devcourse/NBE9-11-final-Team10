package com.team10.backend.domain.youngPolicy.dto.res;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.team10.backend.domain.youngPolicy.entity.YoungPolicy;
import org.springframework.util.StringUtils;

import java.util.List;

// 필요없는 데이터 무시하는 설정 추가하여 시스템 안정성 향상
@JsonIgnoreProperties(ignoreUnknown = true)
public record YoungPolicyExternalRes(
        List<PolicyItem> youthPolicyList // 청년 정책 리스트
) {
    // null 값이 들어올 경우 빈 리스트로 초기화하여 시스템 에러 방지
    public YoungPolicyExternalRes {
        if (youthPolicyList == null) {
            youthPolicyList = List.of();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PolicyItem(
            String plcyNo,         // 정책번호
            String plcyNm,         // 정책명
            String plcyExplnCn,    // 정책 상세한 설명
            String lclsfNm,        // 정책 카테고리 대분류
            String mclsfNm,        // 정책 카테고리 중분류
            String sprtTrgtMinAge, // 최소연령
            String sprtTrgtMaxAge, // 최대연령
            String zipCd,          // 정책거주지역 코드
            String jobCd,          // 정책 취업요건 코드
            String aplyYmd,        // 신청기간
            String aplyUrlAddr,    // 신청URL 주소
            String plcyAplyMthdCn  // 정책신청 방법 설명
    ) {
        public boolean hasPolicyId() {
            return StringUtils.hasText(plcyNo);
        }

        public YoungPolicy toEntity() {
            return new YoungPolicy(
                    plcyNo,
                    plcyNm,
                    plcyExplnCn,
                    lclsfNm,
                    mclsfNm,
                    parseAge(sprtTrgtMinAge),
                    parseAge(sprtTrgtMaxAge),
                    zipCd,
                    jobCd,
                    aplyYmd,
                    aplyUrlAddr,
                    plcyAplyMthdCn
            );
        }

        public void update(YoungPolicy policy) {
            policy.updateFrom(
                    plcyNm,
                    plcyExplnCn,
                    lclsfNm,
                    mclsfNm,
                    parseAge(sprtTrgtMinAge),
                    parseAge(sprtTrgtMaxAge),
                    zipCd,
                    jobCd,
                    aplyYmd,
                    aplyUrlAddr,
                    plcyAplyMthdCn
            );
        }

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
    }
}
