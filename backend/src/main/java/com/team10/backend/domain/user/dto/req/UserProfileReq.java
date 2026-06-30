package com.team10.backend.domain.user.dto.req;

import com.team10.backend.domain.user.type.FinancialInterest;
import jakarta.validation.constraints.Min;
import com.team10.backend.domain.user.type.OccupationStatus;
import com.team10.backend.domain.user.type.Region;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record UserProfileReq(
        @NotNull(message = "태어난 년도는 필수입니다.")
        @Min(value = 1900, message = "올바른 태어난 년도를 입력해주세요.")
        Integer birthYear,

        @NotNull(message = "지역은 필수입니다.")
        Region region,

        @NotNull(message = "직업 상태는 필수입니다.")
        OccupationStatus occupationStatus,

        // 관심분야는 선택 — UserCreateReq와 동일하게 null 허용(UserProfile에서 null-safe 처리)
        Set<FinancialInterest> financialInterests
) {
}
