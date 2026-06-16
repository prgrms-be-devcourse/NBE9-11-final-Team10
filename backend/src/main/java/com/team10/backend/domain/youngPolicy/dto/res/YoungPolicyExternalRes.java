package com.team10.backend.domain.youngPolicy.dto.res;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.team10.backend.domain.youngPolicy.entity.YoungPolicy;
import org.springframework.util.StringUtils;

import java.util.List;

// 청년센터 응답을 담습니다. 모르는 필드는 무시해 API 변경에 대비합니다.
@JsonIgnoreProperties(ignoreUnknown = true)
public record YoungPolicyExternalRes(
        List<PolicyItem> youthPolicyList, // 기존 공개 API 응답 구조
        Result result // 현재 청년센터 정책 API 응답 구조
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
        // 현재 응답 구조를 먼저 보고, 없으면 기존 응답 구조를 사용합니다.
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

        // 외부 API 응답을 DB 엔티티로 바꿉니다.
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

        // 이미 저장된 정책이면 최신 값으로 덮어씁니다.
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
            if (value == null) {
                return null;
            }
            if (value instanceof Number number) {
                return number.intValue();
            }

            String strValue = value.toString();
            if (!StringUtils.hasText(strValue)) {
                return null;
            }

            try {
                // "19.0"처럼 내려온 값도 19로 저장합니다.
                return (int) Double.parseDouble(strValue);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        private String firstText(String primary, String fallback) {
            // 기존 필드가 비어 있으면 새 API 필드를 사용합니다.
            return StringUtils.hasText(primary) ? primary : fallback;
        }
    }
}
