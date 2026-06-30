package com.team10.backend.domain.user.dto.res;

import com.team10.backend.domain.user.entity.UserProfile;
import com.team10.backend.domain.user.type.FinancialInterest;
import com.team10.backend.domain.user.type.OccupationStatus;
import com.team10.backend.domain.user.type.Region;

import java.util.Set;

public record UserProfileRes(
        Long userId,
        Integer birthYear,
        Region region,
        OccupationStatus occupationStatus,
        Set<FinancialInterest> financialInterests
) {
    public static UserProfileRes from(Long userId, UserProfile profile) {
        return new UserProfileRes(
                userId,
                profile.getBirthYear(),
                profile.getRegion(),
                profile.getOccupationStatus(),
                profile.getFinancialInterests()
        );
    }
}
