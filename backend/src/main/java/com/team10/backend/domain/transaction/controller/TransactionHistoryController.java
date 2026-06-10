package com.team10.backend.domain.transaction.controller;

import com.team10.backend.domain.transaction.dto.req.TransactionHistorySearchReq;
import com.team10.backend.domain.transaction.dto.res.TransactionHistoryDetailRes;
import com.team10.backend.domain.transaction.dto.res.TransactionHistorySearchRes;
import com.team10.backend.domain.transaction.service.TransactionHistoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}/transactions")
@RequiredArgsConstructor
@Validated
public class TransactionHistoryController {

    private final TransactionHistoryService transactionHistoryService;

    // 거래 내역 다건 조회 [ 필터링 & 페이징 ]
    @GetMapping
    public ResponseEntity<Page<TransactionHistorySearchRes>> getTransactionHistories(
            @PathVariable @Positive Long accountId,
            @RequestParam @Positive Long userId, // TODO : 추후 CustomUserDetails 로 변경
            @Valid @ModelAttribute TransactionHistorySearchReq filter,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection
    ) {
        return ResponseEntity.ok(
                transactionHistoryService.getTransactionHistories(accountId, userId, filter, page, sortDirection)
        );
    }

    // 거래 내역 단건 조회 [ 상세 정보 ]
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionHistoryDetailRes> getTransactionHistoryDetail(
            @PathVariable @Positive Long accountId,
            @PathVariable @Positive Long transactionId,
            @RequestParam @Positive Long userId // TODO : 추후 CustomUserDetails 로 변경
    ) {
        return ResponseEntity.ok(
                transactionHistoryService.getTransactionHistoryDetail(accountId, transactionId, userId)
        );
    }

}
