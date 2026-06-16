package com.team10.backend.domain.youngPolicy.dto.res;

public record YoungPolicySyncRes(
        int fetchedCount, // 외부 API에서 가져온 전체 정책 수
        int createdCount, // DB에 새로 저장한 정책 수
        int updatedCount, // DB에 있어 갱신한 정책 수
        int skippedCount // 정책번호 누락 등으로 저장하지 않은 정책 수
) {
}
