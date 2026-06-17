package com.team10.backend.domain.investment.account.controller;

import com.team10.backend.domain.investment.account.dto.req.InvestmentAccountCreateReq;
import com.team10.backend.domain.investment.account.dto.res.InvestmentAccountCreateRes;
import com.team10.backend.domain.investment.account.dto.res.InvestmentAccountOpenVerificationRes;
import com.team10.backend.domain.investment.account.service.InvestmentAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
}
