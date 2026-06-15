package com.team10.backend.domain.youngPolicy.dto.res;

import com.team10.backend.domain.youngPolicy.entity.YoungPolicy;

public record YoungPolicyDetailRes(
        Long id,
        String policyId,
        String title,
        String description,
        String category,
        String subCategory,
        Integer minAge,
        Integer maxAge,
        String regionCode,
        String jobCode,
        String applyPeriod,
        String applyUrl,
        String applyMethod
) {
    public static YoungPolicyDetailRes from(YoungPolicy youngPolicy) {
        return new YoungPolicyDetailRes(
                youngPolicy.getId(),
                youngPolicy.getPolicyId(),
                youngPolicy.getTitle(),
                youngPolicy.getDescription(),
                youngPolicy.getCategory(),
                youngPolicy.getSubCategory(),
                youngPolicy.getMinAge(),
                youngPolicy.getMaxAge(),
                youngPolicy.getRegionCode(),
                youngPolicy.getJobCode(),
                youngPolicy.getApplyPeriod(),
                youngPolicy.getApplyUrl(),
                youngPolicy.getApplyMethod()
        );
    }
}
