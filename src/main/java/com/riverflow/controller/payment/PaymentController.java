package com.riverflow.controller.payment;

import com.riverflow.config.jwt.CustomUserDetailsService;
import com.riverflow.model.User;
import com.riverflow.service.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final CustomUserDetailsService userDetailsService;

    @GetMapping("/history")
    public ResponseEntity<List<Map<String, Object>>> getHistory(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        log.info("Fetching payment history for user: {}", userId);
        var response = paymentService.getPaymentHistory(userId);
        return ResponseEntity.ok(response);
    }

    private Long getUserIdFromAuth(Authentication authentication) {
        if (authentication == null) return null;
        String email = authentication.getName();
        User user = userDetailsService.loadUserEntityByEmail(email);
        return user.getId();
    }
}

