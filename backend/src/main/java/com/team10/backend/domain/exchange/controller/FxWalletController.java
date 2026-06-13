package com.team10.backend.domain.exchange.controller;

import com.team10.backend.domain.exchange.dto.res.FxWalletRes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fx-wallets")
@Tag(name = "FxWalletController", description = "외화 지갑 API")
@RequiredArgsConstructor
public class FxWalletController {

    @PostMapping
    @Operation(description = "외화 지갑 생성")
    public ResponseEntity<FxWalletRes> createFxWallet() {
        throw new UnsupportedOperationException("구현 예정 기능");
    }

    @GetMapping
    @Operation(description = "내 외화 지갑 목록 조회")
    public ResponseEntity<List<FxWalletRes>> getFxWallets() {
        throw new UnsupportedOperationException("구현 예정 기능");
    }

    @GetMapping("/{fxWalletId}")
    @Operation(description = "외화 지갑 상세 조회")
    public ResponseEntity<FxWalletRes> getFxWallet() {
        throw new UnsupportedOperationException("구현 예정 기능");
    }

    @PostMapping("/{fxWalletId}/close")
    @Operation(description = "외화 지갑 해지/비활성화")
    public ResponseEntity<FxWalletRes> closeFxWallet() {
        throw new UnsupportedOperationException("구현 예정 기능");
    }
}
