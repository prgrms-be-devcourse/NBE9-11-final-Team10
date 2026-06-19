package com.team10.backend.domain.exAccount.controller;

import com.team10.backend.domain.exAccount.dto.res.ExAccountRes;
import com.team10.backend.domain.exAccount.service.ExAccountService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/external-accounts")
@Tag(name = "External Account", description = "외부 계좌 API")
public class ExAccountController {
    private final ExAccountService exAccountService;

    //외부 계좌 조회
    @GetMapping("/accounts")
    public ResponseEntity<List<ExAccountRes>> getAccounts(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(exAccountService.getAccounts(userId));
    }




}
