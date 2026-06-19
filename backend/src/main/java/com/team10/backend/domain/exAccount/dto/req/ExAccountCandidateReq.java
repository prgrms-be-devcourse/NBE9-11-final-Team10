package com.team10.backend.domain.exAccount.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "외부 계좌 조회 후보 요청")
public record ExAccountCandidateReq(
        @Schema(description = "외부기관에서 조회된 외부 계좌 목록")
        @Valid
        @NotEmpty(message = "조회된 외부 계좌 목록은 필수입니다.")
        List<ExAccountLinkReq> accounts
) {
}
