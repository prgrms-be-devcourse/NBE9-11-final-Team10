package com.team10.backend.domain.investment.account.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team10.backend.domain.investment.account.dto.req.InvestmentAccountCreateReq;
import com.team10.backend.domain.investment.account.dto.res.InvestmentAccountCreateRes;
import com.team10.backend.domain.investment.account.dto.res.InvestmentAccountOpenVerificationRes;
import com.team10.backend.domain.investment.account.service.InvestmentAccountService;
import com.team10.backend.domain.investment.account.type.InvestmentAccountStatus;
import com.team10.backend.domain.investment.type.CurrencyCode;
import com.team10.backend.support.security.AuthenticationPrincipalTestConfig;
import com.team10.backend.support.security.WithMockLongUser;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InvestmentAccountController.class)
@Import(AuthenticationPrincipalTestConfig.class)
@WithMockLongUser
class InvestmentAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private InvestmentAccountService investmentAccountService;

    @Test
    @DisplayName("투자 계좌 개설 인증키 발급 API는 인증 사용자를 받아 200을 반환한다")
    void issueOpenVerificationKey() throws Exception {
        InvestmentAccountOpenVerificationRes response = new InvestmentAccountOpenVerificationRes(
                "verification-key",
                600L
        );

        when(investmentAccountService.issueOpenVerificationKey(1L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/investment/accounts/open-verification"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationKey").value("verification-key"))
                .andExpect(jsonPath("$.expiresInSeconds").value(600L));

        verify(investmentAccountService).issueOpenVerificationKey(1L);
    }

    @Test
    @DisplayName("투자 계좌 개설 API는 인증 사용자와 요청 본문을 받아 201을 반환한다")
    void createAccount() throws Exception {
        InvestmentAccountCreateReq request =
                new InvestmentAccountCreateReq("모의투자 계좌", "123456", "verification-key", CurrencyCode.KRW);
        InvestmentAccountCreateRes response = new InvestmentAccountCreateRes(
                "1234567890-12",
                "모의투자 계좌",
                0L,
                CurrencyCode.KRW,
                InvestmentAccountStatus.ACTIVE,
                LocalDateTime.of(2026, 6, 17, 10, 30)
        );

        when(investmentAccountService.createAccount(eq(1L), any(InvestmentAccountCreateReq.class))).thenReturn(
                response);

        mockMvc.perform(post("/api/v1/investment/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNumber").value("1234567890-12"))
                .andExpect(jsonPath("$.nickname").value("모의투자 계좌"))
                .andExpect(jsonPath("$.cashBalance").value(0L))
                .andExpect(jsonPath("$.currencyCode").value("KRW"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(investmentAccountService).createAccount(eq(1L), any(InvestmentAccountCreateReq.class));
    }

    @Test
    @DisplayName("투자 계좌 개설 API는 비밀번호가 숫자 6자리가 아니면 400을 반환한다")
    void createAccountWithInvalidPassword() throws Exception {
        InvestmentAccountCreateReq request =
                new InvestmentAccountCreateReq("모의투자 계좌", "12345a", "verification-key", CurrencyCode.KRW);

        mockMvc.perform(post("/api/v1/investment/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("투자 계좌 개설 API는 개설 인증키가 없으면 400을 반환한다")
    void createAccountWithoutVerificationKey() throws Exception {
        InvestmentAccountCreateReq request =
                new InvestmentAccountCreateReq("모의투자 계좌", "123456", null, CurrencyCode.KRW);

        mockMvc.perform(post("/api/v1/investment/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("투자 계좌 개설 API는 화폐 종류가 없으면 400을 반환한다.")
    void createAccountWithInvalidCurrencyCode() throws Exception {
        InvestmentAccountCreateReq request =
                new InvestmentAccountCreateReq("모의투자 계좌", "123456", "verification-key", null);

        mockMvc.perform(post("/api/v1/investment/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
