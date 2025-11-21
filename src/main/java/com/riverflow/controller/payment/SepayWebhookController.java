package com.riverflow.controller.payment;

import com.riverflow.dto.payment.SepayWebhookPayload;
import com.riverflow.service.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/web-hook")
@RequiredArgsConstructor
@Slf4j
public class SepayWebhookController {

    private final PaymentService paymentService;

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("ok");
    }

    @PostMapping("/sepay")
    public ResponseEntity<Void> handleSepayWebhook(@RequestHeader(value = "X-Api-Key", required = false) String xApiKey,
                                                   @RequestHeader(value = "Api-Key", required = false) String apiKey,
                                                   @RequestBody SepayWebhookPayload payload) {
        String key = xApiKey != null ? xApiKey : apiKey;
        log.info("Webhook sepay received id={}, hasKey={}", payload.getId(), key != null);
        paymentService.handleSepayWebhook(payload, key);
        return ResponseEntity.ok().build();
    }
}
