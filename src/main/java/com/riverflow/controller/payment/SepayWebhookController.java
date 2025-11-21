package com.riverflow.controller.payment;

import com.riverflow.dto.payment.SepayWebhookPayload;
import com.riverflow.service.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/web-hook")
@RequiredArgsConstructor
public class SepayWebhookController {

    private final PaymentService paymentService;

    @PostMapping("/sepay")
    public ResponseEntity<Void> handleSepayWebhook(@RequestHeader(value = "X-Api-Key", required = false) String xApiKey,
                                                   @RequestHeader(value = "Api-Key", required = false) String apiKey,
                                                   @RequestBody SepayWebhookPayload payload) {
        String key = xApiKey != null ? xApiKey : apiKey;
        paymentService.handleSepayWebhook(payload, key);
        return ResponseEntity.ok().build();
    }
}
