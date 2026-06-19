package com.team10.backend.domain.exAccount.controller;

import com.team10.backend.domain.exAccount.dto.req.ExAccountCandidateReq;
import com.team10.backend.domain.exAccount.dto.req.ExAccountLinkReq;
import com.team10.backend.domain.exAccount.dto.req.ExAccountTransactionRefreshReq;
import com.team10.backend.domain.exAccount.dto.res.ExAccountCandidateRes;
import com.team10.backend.domain.exAccount.dto.res.ExAccountDetailRes;
import com.team10.backend.domain.exAccount.dto.res.ExAccountRes;
import com.team10.backend.domain.exAccount.dto.res.ExAccountTransactionRefreshRes;
import com.team10.backend.domain.exAccount.service.ExAccountService;
import com.team10.backend.domain.exAccount.service.ExAccountSyncService;
import com.team10.backend.domain.exAccount.service.ExAccountTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/external-accounts")
@Tag(name = "External Account", description = "외부 계좌 API")
public class ExAccountController {
    private final ExAccountService exAccountService;
    private final ExAccountSyncService exAccountSyncService;
    private final ExAccountTransactionService exAccountTransactionService;

    @Operation(summary = "연동된 외부 계좌 목록 조회", description = "인증 사용자가 연동 버튼으로 저장한 외부 계좌 목록을 조회합니다.")
    @GetMapping("/accounts")
    public ResponseEntity<List<ExAccountRes>> getAccounts(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(exAccountService.getAccounts(userId));
    }

    @Operation(summary = "외부 계좌 상세 및 거래내역 조회", description = "인증 사용자가 선택한 외부 계좌의 상세 정보와 해당 계좌 거래내역을 함께 조회합니다.")
    @GetMapping("/accounts/{exAccountId}")
    public ResponseEntity<ExAccountDetailRes> getAccountDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long exAccountId
    ) {
        return ResponseEntity.ok(exAccountService.getAccountDetail(userId, exAccountId));
    }

    @Operation(summary = "외부 계좌 거래내역 새로고침", description = "외부기관에서 새로 조회한 거래내역을 선택한 외부 계좌에 저장하거나 갱신한 뒤 상세 정보와 거래내역을 반환합니다.")
    @PostMapping("/accounts/{exAccountId}/transactions/refresh")
    public ResponseEntity<ExAccountTransactionRefreshRes> refreshTransactions(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long exAccountId,
            @Valid @RequestBody ExAccountTransactionRefreshReq request
    ) {
        return ResponseEntity.ok(exAccountTransactionService.refreshTransactions(userId, exAccountId, request.transactions()));
    }

    @Operation(summary = "외부 계좌 후보 조회", description = "외부기관에서 조회된 계좌 목록을 저장하지 않고 연동 후보로 반환합니다.")
    @PostMapping("/candidates")
    public ResponseEntity<List<ExAccountCandidateRes>> getLinkCandidates(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ExAccountCandidateReq request
    ) {
        return ResponseEntity.ok(exAccountSyncService.getLinkCandidates(userId, request.accounts()));
    }

    @Operation(summary = "외부 계좌 연동", description = "사용자가 연동 버튼을 누른 외부 계좌만 DB에 저장하거나 최신 스냅샷으로 갱신합니다.")
    @PostMapping("/link")
    public ResponseEntity<ExAccountRes> linkAccount(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ExAccountLinkReq request
    ) {
        ExAccountRes response = exAccountSyncService.linkAccount(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
