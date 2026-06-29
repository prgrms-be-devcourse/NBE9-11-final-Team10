package com.team10.backend.domain.user.application.dto.res;

import com.team10.backend.domain.user.domain.entity.UserConsent;
import com.team10.backend.domain.user.domain.type.TermsType;

import java.time.LocalDateTime;

public record ConsentRes(
        TermsType termsType,
        Boolean agreed,
        LocalDateTime agreedAt
) {
    public static ConsentRes from(UserConsent consent) {
        return new ConsentRes(
                consent.getTermsType(),
                consent.getAgreed(),
                consent.getAgreedAt()
        );
    }
}
