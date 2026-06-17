package com.team10.backend.domain.saving.controller;

import com.team10.backend.domain.saving.dto.req.DepositCreateReq;
import com.team10.backend.domain.saving.dto.res.DepositCreateRes;
import com.team10.backend.domain.saving.service.SavingDepositService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/v1/savings")
@Tag(name = "Saving Deposit", description = "예금 가입 API")
public class SavingDepositController {

    private final SavingDepositService savingDepositService;

    @Operation(summary = "예금 가입", description = "인증 사용자가 활성 예금 상품에 가입합니다.")
    @PostMapping("/deposits")
    public ResponseEntity<DepositCreateRes> createDeposit(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody DepositCreateReq request
    ) {
        DepositCreateRes response =
                savingDepositService.createDeposit(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
