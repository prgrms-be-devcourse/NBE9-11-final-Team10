package com.team10.backend.domain.saving.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team10.backend.domain.saving.dto.req.DepositCreateReq;
import com.team10.backend.domain.saving.dto.res.DepositCreateRes;
import com.team10.backend.domain.saving.service.SavingDepositService;
import com.team10.backend.domain.saving.type.DepositStatus;
import com.team10.backend.support.security.AuthenticationPrincipalTestConfig;
import com.team10.backend.support.security.WithMockLongUser;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SavingDepositController.class)
@Import(AuthenticationPrincipalTestConfig.class)
@WithMockLongUser
class SavingDepositControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private SavingDepositService savingDepositService;

    @Test
    @DisplayName("예금 가입 API는 인증 사용자와 요청 본문을 받아 201을 반환한다")
    void createDeposit() throws Exception {
        DepositCreateReq request = new DepositCreateReq(1L, 1L, 1000000L);
        DepositCreateRes response = new DepositCreateRes(
                1L,
                DepositStatus.ACTIVE,
                1000000L,
                LocalDate.of(2027, 6, 17),
                35000L
        );

        when(savingDepositService.createDeposit(eq(1L), any(DepositCreateReq.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/savings/deposits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.depositId").value(1L))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.principal").value(1000000L))
                .andExpect(jsonPath("$.maturityDate").value("2027-06-17"))
                .andExpect(jsonPath("$.expectedInterest").value(35000L));

        verify(savingDepositService).createDeposit(eq(1L), any(DepositCreateReq.class));
    }

    @Test
    @DisplayName("예금 가입 API는 필수값이 없으면 400을 반환한다")
    void createDepositWithoutRequiredValue() throws Exception {
        DepositCreateReq request = new DepositCreateReq(null, 1L, 1000000L);

        mockMvc.perform(post("/api/v1/savings/deposits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
