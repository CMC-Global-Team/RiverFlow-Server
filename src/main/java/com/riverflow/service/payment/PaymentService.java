package com.riverflow.service.payment;

import com.riverflow.dto.payment.SepayWebhookPayload;
import com.riverflow.model.User;
import com.riverflow.model.payment.CreditTopupRequest;
import com.riverflow.model.payment.PaymentTransaction;
import com.riverflow.repository.UserRepository;
import com.riverflow.repository.payment.CreditTopupRequestRepository;
import com.riverflow.repository.payment.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final CreditTopupRequestRepository topupRequestRepository;
    private final UserRepository userRepository;
    private final PaymentTransactionRepository transactionRepository;

    @Value("${app.sepay.api-key:}")
    private String sepayApiKey;

    @Value("${app.sepay.account-number}")
    private String accountNumber;

    @Value("${app.sepay.bank}")
    private String bank;

    public CreditTopupRequest createTopupRequest(Long userId, Long amount) {
        User user = userRepository.findById(userId).orElseThrow();
        String code = generateUniqueCode();
        CreditTopupRequest req = CreditTopupRequest.builder()
                .user(user)
                .code(code)
                .amount(amount)
                .status(CreditTopupRequest.TopupStatus.pending)
                .build();
        return topupRequestRepository.save(req);
    }

    public String buildSepayQrUrl(Long amount, String code) {
        String template = "compact";
        String download = "0";
        String base = "https://qr.sepay.vn/img";
        return base + "?acc=" + accountNumber + "&bank=" + bank + "&amount=" + amount + "&des=" + code + "&template=" + template + "&download=" + download;
    }

    @Transactional
    public void handleSepayWebhook(SepayWebhookPayload payload, String apiKeyHeader) {
        if (apiKeyHeader == null || sepayApiKey == null || !sepayApiKey.equals(apiKeyHeader)) {
            return;
        }
        String codeCandidate = payload.getCode();
        if ((codeCandidate == null || codeCandidate.isEmpty()) && payload.getContent() != null) {
            codeCandidate = extractCodeFromContent(payload.getContent());
        }
        PaymentTransaction.TransferType type = payload.getTransferType() != null && payload.getTransferType().equalsIgnoreCase("in")
                ? PaymentTransaction.TransferType.in
                : PaymentTransaction.TransferType.out;
        PaymentTransaction tx = PaymentTransaction.builder()
                .externalId(payload.getId())
                .gateway(payload.getGateway() != null ? payload.getGateway() : "sepay")
                .transactionDate(parseDate(payload.getTransactionDate()))
                .accountNumber(payload.getAccountNumber())
                .code(codeCandidate)
                .content(payload.getContent())
                .transferType(type)
                .transferAmount(payload.getTransferAmount())
                .accumulated(payload.getAccumulated())
                .subAccount(payload.getSubAccount())
                .referenceCode(payload.getReferenceCode())
                .description(payload.getDescription())
                .status(PaymentTransaction.TransactionStatus.pending)
                .build();
        if (type != PaymentTransaction.TransferType.in) {
            tx.setStatus(PaymentTransaction.TransactionStatus.ignored);
            transactionRepository.save(tx);
            return;
        }
        String code = codeCandidate;
        if (code == null || code.isEmpty()) {
            tx.setStatus(PaymentTransaction.TransactionStatus.invalid);
            transactionRepository.save(tx);
            return;
        }
        Optional<CreditTopupRequest> opt = topupRequestRepository.findByCode(code);
        if (opt.isEmpty()) {
            tx.setStatus(PaymentTransaction.TransactionStatus.ignored);
            transactionRepository.save(tx);
            return;
        }
        CreditTopupRequest req = opt.get();
        if (req.getStatus() == CreditTopupRequest.TopupStatus.paid) {
            tx.setMatchedRequest(req);
            tx.setUser(req.getUser());
            tx.setStatus(PaymentTransaction.TransactionStatus.ignored);
            transactionRepository.save(tx);
            return;
        }
        if (!req.getAmount().equals(payload.getTransferAmount())) {
            tx.setMatchedRequest(req);
            tx.setUser(req.getUser());
            tx.setStatus(PaymentTransaction.TransactionStatus.invalid);
            transactionRepository.save(tx);
            return;
        }
        User user = req.getUser();
        long current = user.getCredit() == null ? 0L : user.getCredit();
        user.setCredit(current + payload.getTransferAmount());
        req.setStatus(CreditTopupRequest.TopupStatus.paid);
        req.setPaidAt(LocalDateTime.now());
        userRepository.save(user);
        topupRequestRepository.save(req);
        tx.setMatchedRequest(req);
        tx.setUser(user);
        tx.setStatus(PaymentTransaction.TransactionStatus.processed);
        transactionRepository.save(tx);
    }

    private String generateUniqueCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        } while (topupRequestRepository.findByCode(code).isPresent());
        return code;
    }

    private String extractCodeFromContent(String content) {
        String cleaned = content == null ? "" : content.toUpperCase();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("[A-Z0-9]{10,16}").matcher(cleaned);
        if (m.find()) {
            return m.group(0);
        }
        return null;
    }

    private LocalDateTime parseDate(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        try {
            return java.time.OffsetDateTime.parse(s).toLocalDateTime();
        } catch (Exception ignored) {
        }
        try {
            java.time.format.DateTimeFormatter f = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return LocalDateTime.parse(s, f);
        } catch (Exception ignored) {
        }
        return null;
    }
}
