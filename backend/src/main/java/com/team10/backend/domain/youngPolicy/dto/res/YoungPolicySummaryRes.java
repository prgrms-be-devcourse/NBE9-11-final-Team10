package com.team10.backend.domain.youngPolicy.dto.res;

import com.team10.backend.domain.youngPolicy.entity.YoungPolicy;

public record YoungPolicySummaryRes(
        Long id,
        String policyId,
        String title,
        String category,
        String subCategory,
        Integer minAge,
        Integer maxAge,
        String regionCode,
        String applyPeriod
) {
    public static YoungPolicySummaryRes from(YoungPolicy youngPolicy) {
        return new YoungPolicySummaryRes(
                youngPolicy.getId(),
                youngPolicy.getPolicyId(),
                youngPolicy.getTitle(),
                youngPolicy.getCategory(),
                youngPolicy.getSubCategory(),
                youngPolicy.getMinAge(),
                youngPolicy.getMaxAge(),
                youngPolicy.getRegionCode(),
                youngPolicy.getApplyPeriod()
        );
    }
}
