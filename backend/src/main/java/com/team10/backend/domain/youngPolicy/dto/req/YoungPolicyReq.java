package com.team10.backend.domain.youngPolicy.dto.req;

public record YoungPolicyReq(
        Integer pageNum,
        Integer pageSize
        // 필요한 검색 조건 이후에 추가
) {
}
