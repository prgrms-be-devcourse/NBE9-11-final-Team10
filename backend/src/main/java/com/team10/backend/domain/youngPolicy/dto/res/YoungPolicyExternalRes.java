package com.team10.backend.domain.youngPolicy.dto.res;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.team10.backend.domain.youngPolicy.entity.YoungPolicy;
import org.springframework.util.StringUtils;

import java.util.List;

// 필요없는 데이터 무시하는 설정 추가하여 시스템 안정성 향상
@JsonIgnoreProperties(ignoreUnknown = true)
public record YoungPolicyExternalRes(
        List<PolicyItem> youthPolicyList, // 기존 공개 API 응답 필드
        Result result // 현재 청년센터 정책 API 응답 필드
) {
    public YoungPolicyExternalRes(List<PolicyItem> youthPolicyList) {
        this(youthPolicyList, null);
    }

    public YoungPolicyExternalRes {
        if (youthPolicyList == null) {
            youthPolicyList = List.of();
        }
    }

    public List<PolicyItem> policyItems() {
        if (result != null && result.plcyList() != null) {
            return result.plcyList();
        }
        return youthPolicyList;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(
            List<PolicyItem> plcyList
    ) {
        public Result {
            if (plcyList == null) {
                plcyList = List.of();
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PolicyItem(
            String plcyNo,         // 정책번호
            String plcyNm,         // 정책명
            String plcyExplnCn,    // 정책 상세한 설명
            String lclsfNm,        // 정책 카테고리 대분류
            String userLclsfNm,    // 현재 API 정책 카테고리 대분류
            String mclsfNm,        // 정책 카테고리 중분류
            Object sprtTrgtMinAge, // 최소연령
            Object sprtTrgtMaxAge, // 최대연령
            String zipCd,          // 정책거주지역 코드
            String stdgCtpvSggCdList, // 현재 API 정책거주지역 목록
            String jobCd,          // 정책 취업요건 코드
            String aplyYmd,        // 신청기간
            String aplyPeriod,     // 현재 API 신청기간
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
                    firstText(lclsfNm, userLclsfNm),
                    mclsfNm,
                    parseAge(sprtTrgtMinAge),
                    parseAge(sprtTrgtMaxAge),
                    firstText(zipCd, stdgCtpvSggCdList),
                    jobCd,
                    firstText(aplyYmd, aplyPeriod),
                    aplyUrlAddr,
                    plcyAplyMthdCn
            );
        }

        public void update(YoungPolicy policy) {
            policy.updateFrom(
                    plcyNm,
                    plcyExplnCn,
                    firstText(lclsfNm, userLclsfNm),
                    mclsfNm,
                    parseAge(sprtTrgtMinAge),
                    parseAge(sprtTrgtMaxAge),
                    firstText(zipCd, stdgCtpvSggCdList),
                    jobCd,
                    firstText(aplyYmd, aplyPeriod),
                    aplyUrlAddr,
                    plcyAplyMthdCn
            );
        }

        private Integer parseAge(Object value) {
            if (value == null || !StringUtils.hasText(value.toString())) {
                return null;
            }

            try {
                return Integer.valueOf(value.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }

        private String firstText(String primary, String fallback) {
            return StringUtils.hasText(primary) ? primary : fallback;
        }
    }
}
