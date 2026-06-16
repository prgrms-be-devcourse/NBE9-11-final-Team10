package com.team10.backend.domain.youngPolicy.dto.req;

public record YoungPolicyReq(
        // 청년센터 API 페이지 번호와 조회 개수입니다.
        Integer pageNum,
        Integer pageSize
) {
}
