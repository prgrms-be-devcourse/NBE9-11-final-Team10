package com.team10.backend.domain.account.dto.req;

import com.team10.backend.domain.account.type.AccountType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AccountCreateReq {

    @Size(max = 50, message = "계좌 별칭은 50자 이하여야 합니다.")
    private String nickname;

    @NotNull(message = "계좌 타입은 필수입니다.")
    private AccountType accountType;
}