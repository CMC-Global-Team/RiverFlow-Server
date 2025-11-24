package com.riverflow.service.payment;

import java.util.List;
import java.util.Map;

public interface PaymentService {
    List<Map<String, Object>> getPaymentHistory(Long userId);
    void handleSepayWebhook(com.riverflow.dto.payment.SepayWebhookPayload payload, String apiKey);
}
