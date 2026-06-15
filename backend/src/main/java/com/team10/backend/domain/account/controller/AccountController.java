package com.team10.backend.domain.account.controller;

import com.team10.backend.domain.account.dto.req.AccountCreateReq;
import com.team10.backend.domain.account.dto.req.AccountNicknameUpdateReq;
import com.team10.backend.domain.account.dto.res.AccountCreateRes;
import com.team10.backend.domain.account.dto.res.AccountDetailRes;
import com.team10.backend.domain.account.dto.res.AccountSummaryRes;
import com.team10.backend.domain.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountCreateRes> createAccount(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody AccountCreateReq request
    ) {
        AccountCreateRes response = accountService.createAccount(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{accountId}/nickname")
    public ResponseEntity<AccountDetailRes> updateNickname(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long accountId,
            @Valid @RequestBody AccountNicknameUpdateReq request
    ){
        AccountDetailRes response = accountService.updateNickname(userId, accountId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{accountId}/close")
    public ResponseEntity<AccountDetailRes> closeAccount(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long accountId
    ){
        AccountDetailRes response = accountService.closeAccount(userId, accountId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<AccountSummaryRes>> getAccounts(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(accountService.getAccounts(userId));
    }

    @GetMapping("/closed")
    public ResponseEntity<List<AccountSummaryRes>> getClosedAccounts(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(accountService.getClosedAccounts(userId));
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountDetailRes> getAccount(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long accountId
    ) {
        return ResponseEntity.ok(accountService.getAccount(userId, accountId));
    }
}
