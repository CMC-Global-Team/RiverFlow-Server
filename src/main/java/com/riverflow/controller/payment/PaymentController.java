package com.riverflow.controller.payment;

import com.riverflow.config.jwt.CustomUserDetailsService;
import com.riverflow.config.jwt.UserPrincipal;
import com.riverflow.dto.payment.CreateTopupIntentRequest;
import com.riverflow.dto.payment.PaymentHistoryResponse;
import com.riverflow.dto.payment.TopupIntentResponse;
import com.riverflow.model.payment.CreditTopupRequest;
import com.riverflow.service.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final CustomUserDetailsService userDetailsService;

    @PostMapping("/topup-intent")
    public ResponseEntity<TopupIntentResponse> createTopupIntent(@RequestBody CreateTopupIntentRequest request,
                                                                 Authentication authentication) {
        Long userId = userDetailsService.loadUserEntityByEmail(authentication.getName()).getId();
        CreditTopupRequest req = paymentService.createTopupRequest(userId, request.getAmount());
        String qrUrl = paymentService.buildSepayQrUrl(req.getAmount(), req.getCode());
        return ResponseEntity.ok(new TopupIntentResponse(req.getCode(), req.getAmount(), qrUrl));
    }

    /**
     * API Lấy lịch sử giao dịch
     * GET /api/payments/history?page=0&size=10
     */
    @GetMapping("/history")
    public ResponseEntity<Page<PaymentHistoryResponse>> getPaymentHistory(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long userId = userDetailsService.loadUserEntityByEmail(authentication.getName()).getId();
        Page<PaymentHistoryResponse> history = paymentService.getUserTransactions(userId, page, size);
        return ResponseEntity.ok(history);
    }
}
