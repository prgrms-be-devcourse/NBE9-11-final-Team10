package com.team10.backend.domain.investment.account.controller;

import com.team10.backend.domain.investment.account.dto.req.InvestmentAccountCreateReq;
import com.team10.backend.domain.investment.account.dto.req.InvestmentAccountUpdateReq;
import com.team10.backend.domain.investment.account.dto.res.InvestmentAccountCreateRes;
import com.team10.backend.domain.investment.account.dto.res.InvestmentAccountOpenVerificationRes;
import com.team10.backend.domain.investment.account.dto.res.InvestmentAccountUpdateRes;
import com.team10.backend.domain.investment.account.service.InvestmentAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/investment/accounts")
@Tag(name = "Investment Account", description = "투자 계좌 API")
public class InvestmentAccountController {

    private final InvestmentAccountService investmentAccountService;

    @Operation(summary = "투자 계좌 개설 인증키 발급", description = "본인인증이 완료된 인증 사용자에게 투자 계좌 개설 인증키를 발급합니다.")
    @PostMapping("/open-verification")
    public ResponseEntity<InvestmentAccountOpenVerificationRes> issueOpenVerificationKey(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(investmentAccountService.issueOpenVerificationKey(userId));
    }

    @Operation(summary = "투자 계좌 개설", description = "본인인증과 개설 인증키 검증을 완료한 인증 사용자의 투자 계좌를 개설합니다.")
    @PostMapping
    public ResponseEntity<InvestmentAccountCreateRes> createAccount(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Long userId,

            @Valid @RequestBody InvestmentAccountCreateReq request
    ) {
        InvestmentAccountCreateRes response = investmentAccountService.createAccount(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "투자 계좌 정보 수정", description = "계좌 비밀번호 검증 후 전달된 별칭 또는 새 비밀번호만 수정합니다.")
    @PatchMapping("/{accountId}")
    public ResponseEntity<InvestmentAccountUpdateRes> updateAccount(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Long userId,

            @PathVariable Long accountId,

            @Valid @RequestBody InvestmentAccountUpdateReq request
    ) {
        InvestmentAccountUpdateRes response = investmentAccountService.updateAccount(userId, accountId, request);
        return ResponseEntity.ok(response);
    }
}
