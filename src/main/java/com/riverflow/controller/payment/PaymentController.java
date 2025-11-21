package com.riverflow.controller.payment;

import com.riverflow.config.jwt.CustomUserDetailsService;
import com.riverflow.dto.payment.CreateTopupIntentRequest;
import com.riverflow.dto.payment.TopupIntentResponse;
import com.riverflow.model.payment.CreditTopupRequest;
import com.riverflow.service.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
}
