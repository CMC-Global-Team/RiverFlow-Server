package com.riverflow.controller.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riverflow.dto.payment.SepayWebhookPayload;
import com.riverflow.service.payment.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller tests for SepayWebhookController using MockMvc
 */
@WebMvcTest(controllers = SepayWebhookController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        })
class SepayWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @Test
    void ping_ReturnsOk() throws Exception {
        // When & Then
        mockMvc.perform(get("/web-hook/ping"))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    @Test
    void handleSepayWebhook_ValidPayload_ProcessesWebhook() throws Exception {
        // Given
        SepayWebhookPayload payload = new SepayWebhookPayload();
        payload.setContent("TOPUP-123");
        payload.setTransferAmount(100000L);

        String apiKey = "test-api-key";
        doNothing().when(paymentService).handleSepayWebhook(any(SepayWebhookPayload.class), eq(apiKey));

        // When & Then
        mockMvc.perform(post("/web-hook/sepay")
                        .header("X-Api-Key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        verify(paymentService).handleSepayWebhook(any(SepayWebhookPayload.class), eq(apiKey));
    }

    @Test
    void handleSepayWebhook_ApiKeyInDifferentHeader_ProcessesWebhook() throws Exception {
        // Given
        SepayWebhookPayload payload = new SepayWebhookPayload();
        String apiKey = "alternate-api-key";
        doNothing().when(paymentService).handleSepayWebhook(any(SepayWebhookPayload.class), eq(apiKey));

        // When & Then
        mockMvc.perform(post("/web-hook/sepay")
                        .header("Api-Key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        verify(paymentService).handleSepayWebhook(any(SepayWebhookPayload.class), eq(apiKey));
    }

    @Test
    void handleSepayWebhook_BearerToken_ExtractsAndProcesses() throws Exception {
        // Given
        SepayWebhookPayload payload = new SepayWebhookPayload();
        String token = "bearer-token-123";
        doNothing().when(paymentService).handleSepayWebhook(any(SepayWebhookPayload.class), eq(token));

        // When & Then
        mockMvc.perform(post("/web-hook/sepay")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        verify(paymentService).handleSepayWebhook(any(SepayWebhookPayload.class), eq(token));
    }
}
