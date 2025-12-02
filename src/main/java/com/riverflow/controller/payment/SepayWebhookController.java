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

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("ok");
    }

    @PostMapping("/sepay")
    public ResponseEntity<Void> handleSepayWebhook(@RequestHeader(value = "X-Api-Key", required = false) String xApiKey,
                                                   @RequestHeader(value = "Api-Key", required = false) String apiKey,
                                                   @RequestHeader(value = "X-Sepay-Api-Key", required = false) String xSepayApiKey,
                                                   @RequestHeader(value = "Api-Access", required = false) String apiAccess,
                                                   @RequestHeader(value = "X-Api-Access", required = false) String xApiAccess,
                                                   @RequestHeader(value = "Authorization", required = false) String authorization,
                                                   @RequestParam(value = "api_key", required = false) String apiKeyParam,
                                                   @RequestParam(value = "api_access", required = false) String apiAccessParam,
                                                   @RequestBody SepayWebhookPayload payload) {
        String bearer = null;
        if (authorization != null) {
            String a = authorization.trim();
            if (a.toLowerCase().startsWith("bearer ")) {
                bearer = a.substring(7).trim();
            } else {
                bearer = a;
            }
        }
        String key = xApiKey != null ? xApiKey :
                (apiKey != null ? apiKey :
                        (xSepayApiKey != null ? xSepayApiKey :
                                (apiKeyParam != null ? apiKeyParam :
                                        (apiAccess != null ? apiAccess :
                                                (xApiAccess != null ? xApiAccess :
                                                        (apiAccessParam != null ? apiAccessParam : bearer))))));
        paymentService.handleSepayWebhook(payload, key);
        return ResponseEntity.ok().build();
    }
}
