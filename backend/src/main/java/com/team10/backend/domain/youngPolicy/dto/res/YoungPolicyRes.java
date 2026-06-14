package com.team10.backend.domain.youngPolicy.dto.res;

import java.util.List;

public record YoungPolicyRes(
        List<PolicyItem> youthPolicyList // 청년 정책 리스트
) {
    // API에서 받아올 핵심 항목들만 선언합니다.
    public record PolicyItem(
            String plcyNo,         // 정책번호
            String plcyNm,         // 정책명
            String plcyExplnCn, //정책 상세한 설명
            String lclsfNm, // 정책 카테고리 대분류
            String mclsfNm, // 정책카 카테고리 중분류
            String sprtTrgtMinAge, // 최소연령
            String sprtTrgtMaxAge,  // 최대연령
            String zipCd, // 정책거주지역 코드
            String jobCd, // 정책 취업요건 코드
            String aplyYmd, // 신청기간
            String aplyUrlAddr, // 신청URL 주소
            String plcyAplyMthdCn //정책신청 방법 설명
    ) {
    }
}
