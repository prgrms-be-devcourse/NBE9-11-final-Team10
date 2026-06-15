package com.team10.backend.domain.youngPolicy.dto.res;

public record YoungPolicySyncRes(
        int fetchedCount, // 청년 정책 API에서 가져온 전체 정책 수
        int createdCount, // DB에 새로 생성된 정책 수
        int updatedCount, // DB에 새로 업데이퇸 정책 수
        int skippedCount // 청년 정책 API에서 가져왔지만 DB에 저장하지 않은 정책 수
        // (이미 존재하는 정책이거나 유효하지 않은 데이터인 경우)
) {
}
