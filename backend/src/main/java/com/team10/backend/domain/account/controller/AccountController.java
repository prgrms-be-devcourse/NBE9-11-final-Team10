package com.team10.backend.domain.account.controller;

import com.team10.backend.domain.account.dto.req.AccountCreateReq;
import com.team10.backend.domain.account.dto.res.AccountRes;
import com.team10.backend.domain.account.dto.res.AccountSummaryRes;
import com.team10.backend.domain.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountRes> createAccount(
            // TODO: 인증 도메인 연동 후 @RequestParam userId를 @AuthenticationPrincipal 기반으로 교체
            @RequestParam Long userId,
            @Valid @RequestBody AccountCreateReq request
    ) {
        AccountRes response = accountService.createAccount(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<AccountSummaryRes>> getAccounts(
            // TODO: 인증 도메인 연동 후 @RequestParam userId를 @AuthenticationPrincipal 기반으로 교체
            @RequestParam Long userId
    ) {
        return ResponseEntity.ok(accountService.getAccounts(userId));
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountRes> getAccount(
            // TODO: 인증 도메인 연동 후 @RequestParam userId를 @AuthenticationPrincipal 기반으로 교체
            @RequestParam Long userId,
            @PathVariable Long accountId
    ) {
        return ResponseEntity.ok(accountService.getAccount(userId, accountId));
    }
}
