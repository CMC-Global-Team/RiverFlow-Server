package com.riverflow.controller.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riverflow.config.jwt.CustomUserDetailsService;
import com.riverflow.config.jwt.UserPrincipal;
import com.riverflow.dto.payment.CreateTopupIntentRequest;
import com.riverflow.dto.payment.PaymentHistoryResponse;
import com.riverflow.dto.payment.TopupIntentResponse;
import com.riverflow.model.User;
import com.riverflow.model.payment.CreditTopupRequest;
import com.riverflow.service.payment.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller tests for PaymentController using MockMvc
 */
@WebMvcTest(controllers = PaymentController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        })
@Import(com.riverflow.config.TestSecurityConfig.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    private User testUser;
    private CreditTopupRequest topupRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .fullName("Test User")
                .build();

        topupRequest = CreditTopupRequest.builder()
                .code("TOPUP-123")
                .amount(100000L)
                .user(testUser)
                .build();
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void createTopupIntent_ValidRequest_ReturnsIntent() throws Exception {
        // Given
        CreateTopupIntentRequest request = new CreateTopupIntentRequest();
        request.setAmount(100000L);

        when(userDetailsService.loadUserEntityByEmail("test@example.com")).thenReturn(testUser);
        when(paymentService.createTopupRequest(1L, 100000L)).thenReturn(topupRequest);
        when(paymentService.buildSepayQrUrl(100000L, "TOPUP-123")).thenReturn("https://qr.sepay.vn/...");

        // When & Then
        mockMvc.perform(post("/payments/topup-intent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TOPUP-123"))
                .andExpect(jsonPath("$.amount").value(100000))
                .andExpect(jsonPath("$.qrUrl").value("https://qr.sepay.vn/..."));

        verify(paymentService).createTopupRequest(1L, 100000L);
        verify(paymentService).buildSepayQrUrl(100000L, "TOPUP-123");
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void getPaymentHistory_AuthenticatedUser_ReturnsHistory() throws Exception {
        // Given
        PaymentHistoryResponse historyItem = new PaymentHistoryResponse();
        historyItem.setTransactionCode("TOPUP-123");
        historyItem.setAmount(100000L);

        Page<PaymentHistoryResponse> page = new PageImpl<>(Collections.singletonList(historyItem));

        when(paymentService.getUserTransactions(1L, 0, 10)).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/payments/history")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].transactionCode").value("TOPUP-123"))
                .andExpect(jsonPath("$.content[0].amount").value(100000));

        verify(paymentService).getUserTransactions(1L, 0, 10);
    }
}
