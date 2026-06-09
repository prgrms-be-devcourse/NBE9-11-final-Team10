package com.team10.backend.domain.transaction.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team10.backend.domain.transaction.dto.req.TransactionHistorySearchReq;
import com.team10.backend.domain.transaction.dto.res.TransactionHistorySearchRes;
import com.team10.backend.domain.transaction.service.TransactionHistoryService;
import com.team10.backend.domain.transaction.type.TransactionDirection;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TransactionHistoryController.class)
class TransactionHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionHistoryService transactionHistoryService;

    @Test
    @DisplayName("다건 조회 성공 시 Page 응답 구조를 반환한다")
    void getTransactionHistoriesSucceedsAndReturnsPageStructure() throws Exception {
        TransactionHistorySearchRes response = new TransactionHistorySearchRes(
                1L,
                "홍길동",
                5_000L,
                95_000L,
                LocalDateTime.of(2026, 6, 9, 12, 30),
                TransactionDirection.OUT
        );
        given(transactionHistoryService.getTransactionHistories(
                eq(1L),
                eq(10L),
                any(TransactionHistorySearchReq.class),
                eq(0),
                eq(Sort.Direction.DESC)
        )).willReturn(new PageImpl<>(
                List.of(response),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "transactedAt")),
                1
        ));

        mockMvc.perform(get("/api/accounts/{accountId}/transactions", 1L)
                        .param("userId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].transactionHistoryId").value(1))
                .andExpect(jsonPath("$.content[0].counterpartyName").value("홍길동"))
                .andExpect(jsonPath("$.content[0].amount").value(5000))
                .andExpect(jsonPath("$.content[0].balanceAfter").value(95000))
                .andExpect(jsonPath("$.content[0].transactedAt").value("2026-06-09T12:30:00"))
                .andExpect(jsonPath("$.content[0].direction").value("OUT"))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.pageable.pageSize").value(20))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(transactionHistoryService).getTransactionHistories(
                eq(1L),
                eq(10L),
                any(TransactionHistorySearchReq.class),
                eq(0),
                eq(Sort.Direction.DESC)
        );
    }

    @Test
    @DisplayName("요청한 page와 정렬 방향을 서비스에 전달한다")
    void getTransactionHistoriesPassesCustomPagingAndSortDirection() throws Exception {
        given(transactionHistoryService.getTransactionHistories(
                eq(1L),
                eq(10L),
                any(TransactionHistorySearchReq.class),
                eq(2),
                eq(Sort.Direction.ASC)
        )).willReturn(new PageImpl<>(
                List.of(),
                PageRequest.of(2, 20, Sort.by(Sort.Direction.ASC, "transactedAt")),
                0
        ));

        mockMvc.perform(get("/api/accounts/{accountId}/transactions", 1L)
                        .param("userId", "10")
                        .param("page", "2")
                        .param("sortDirection", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.pageable.pageNumber").value(2));

        verify(transactionHistoryService).getTransactionHistories(
                eq(1L),
                eq(10L),
                any(TransactionHistorySearchReq.class),
                eq(2),
                eq(Sort.Direction.ASC)
        );
    }

    @Test
    @DisplayName("accountId가 양수가 아니면 400을 반환한다")
    void invalidWhenAccountIdIsNotPositive() throws Exception {
        mockMvc.perform(get("/api/accounts/{accountId}/transactions", 0L)
                        .param("userId", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("userId가 양수가 아니면 400을 반환한다")
    void invalidWhenUserIdIsNotPositive() throws Exception {
        mockMvc.perform(get("/api/accounts/{accountId}/transactions", 1L)
                        .param("userId", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("userId가 없으면 400을 반환한다")
    void invalidWhenUserIdIsMissing() throws Exception {
        mockMvc.perform(get("/api/accounts/{accountId}/transactions", 1L))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("page가 음수이면 400을 반환한다")
    void invalidWhenPageIsNegative() throws Exception {
        mockMvc.perform(get("/api/accounts/{accountId}/transactions", 1L)
                        .param("userId", "10")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("정렬 방향 값이 ASC 또는 DESC가 아니면 400을 반환한다")
    void invalidWhenSortDirectionIsNotEnumValue() throws Exception {
        mockMvc.perform(get("/api/accounts/{accountId}/transactions", 1L)
                        .param("userId", "10")
                        .param("sortDirection", "LATEST"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("입출금 방향 값이 IN 또는 OUT이 아니면 400을 반환한다")
    void invalidWhenDirectionIsNotEnumValue() throws Exception {
        mockMvc.perform(get("/api/accounts/{accountId}/transactions", 1L)
                        .param("userId", "10")
                        .param("direction", "INVALID"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("시작일이 종료일보다 이후이면 400을 반환한다")
    void invalidWhenStartDateIsAfterEndDate() throws Exception {
        mockMvc.perform(get("/api/accounts/{accountId}/transactions", 1L)
                        .param("userId", "10")
                        .param("startDate", "2026-06-10")
                        .param("endDate", "2026-06-09"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("최소 거래액이 최대 거래액보다 크면 400을 반환한다")
    void invalidWhenMinAmountIsGreaterThanMaxAmount() throws Exception {
        mockMvc.perform(get("/api/accounts/{accountId}/transactions", 1L)
                        .param("userId", "10")
                        .param("minAmount", "10000")
                        .param("maxAmount", "1000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("최소 거래액이 음수이면 400을 반환한다")
    void invalidWhenMinAmountIsNegative() throws Exception {
        mockMvc.perform(get("/api/accounts/{accountId}/transactions", 1L)
                        .param("userId", "10")
                        .param("minAmount", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("최대 거래액이 음수이면 400을 반환한다")
    void invalidWhenMaxAmountIsNegative() throws Exception {
        mockMvc.perform(get("/api/accounts/{accountId}/transactions", 1L)
                        .param("userId", "10")
                        .param("maxAmount", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("거래 상대명이 30자를 초과하면 400을 반환한다")
    void invalidWhenCounterpartyNameIsTooLong() throws Exception {
        mockMvc.perform(get("/api/accounts/{accountId}/transactions", 1L)
                        .param("userId", "10")
                        .param("counterpartyName", "a".repeat(31)))
                .andExpect(status().isBadRequest());
    }
}
