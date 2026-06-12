package com.team10.backend.domain.user.dto.res;

import com.team10.backend.domain.user.entity.UserProfile;
import com.team10.backend.domain.user.type.AgeGroup;
import com.team10.backend.domain.user.type.FinancialInterest;
import com.team10.backend.domain.user.type.OccupationStatus;

import java.util.Set;

public record UserProfileRes(
        Long userId,
        AgeGroup ageGroup,
        String region,
        OccupationStatus occupationStatus,
        Set<FinancialInterest> financialInterests
) {
    public static UserProfileRes from(UserProfile profile) {
        return new UserProfileRes(
                profile.getUser().getId(),
                profile.getAgeGroup(),
                profile.getRegion(),
                profile.getOccupationStatus(),
                profile.getFinancialInterests()
        );
    }
}
