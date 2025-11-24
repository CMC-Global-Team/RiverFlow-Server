package com.riverflow.service.payment;

import com.riverflow.model.payment.PaymentTransaction;
import com.riverflow.repository.payment.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentTransactionRepository transactionRepository;

    @Override
    public List<Map<String, Object>> getPaymentHistory(Long userId) {
        List<PaymentTransaction> transactions = transactionRepository.findByUser_IdOrderByTransactionDateDesc(userId);
        return transactions.stream().map(tx -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", tx.getId());
            item.put("gateway", tx.getGateway());
            item.put("transactionDate", tx.getTransactionDate());
            item.put("transferType", tx.getTransferType().name());
            item.put("transferAmount", tx.getTransferAmount());
            item.put("referenceCode", tx.getReferenceCode());
            item.put("status", tx.getStatus().name());
            item.put("description", tx.getDescription());
            item.put("code", tx.getCode());
            item.put("accountNumber", tx.getAccountNumber());
            return item;
        }).collect(Collectors.toList());
    }

    @Override
    public void handleSepayWebhook(com.riverflow.dto.payment.SepayWebhookPayload payload, String apiKey) {
        log.info("Sepay webhook received id={}, hasKey={}", payload.getId(), apiKey != null);
    }
}
