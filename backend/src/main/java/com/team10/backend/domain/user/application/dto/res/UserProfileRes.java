package com.team10.backend.domain.user.application.dto.res;


import com.team10.backend.domain.user.domain.entity.UserProfile;
import com.team10.backend.domain.user.domain.type.AgeGroup;
import com.team10.backend.domain.user.domain.type.FinancialInterest;
import com.team10.backend.domain.user.domain.type.OccupationStatus;
import com.team10.backend.domain.user.domain.type.Region;

import java.util.Set;

public record UserProfileRes(
        Long userId,
        AgeGroup ageGroup,
        Region region,
        OccupationStatus occupationStatus,
        Set<FinancialInterest> financialInterests
) {
    public static UserProfileRes from(Long userId, UserProfile profile) {
        return new UserProfileRes(
                userId,
                profile.getAgeGroup(),
                profile.getRegion(),
                profile.getOccupationStatus(),
                profile.getFinancialInterests()
        );
    }
}
