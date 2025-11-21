package com.riverflow.service.payment;

import com.riverflow.dto.payment.SepayWebhookPayload;
import com.riverflow.model.User;
import com.riverflow.model.payment.CreditTopupRequest;
import com.riverflow.model.payment.PaymentTransaction;
import com.riverflow.repository.UserRepository;
import com.riverflow.repository.payment.CreditTopupRequestRepository;
import com.riverflow.repository.payment.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final CreditTopupRequestRepository topupRequestRepository;
    private final UserRepository userRepository;
    private final PaymentTransactionRepository transactionRepository;

    @Value("${app.sepay.api-key:}")
    private String sepayApiKey;

    @Value("${app.sepay.api-access:}")
    private String sepayApiAccess;

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
        String codeCandidate = payload.getCode();
        if ((codeCandidate == null || codeCandidate.isEmpty()) && payload.getContent() != null) {
            java.util.List<String> candidates = extractCodesFromContent(payload.getContent());
            for (String c : candidates) {
                if (topupRequestRepository.findByCode(c).isPresent()) {
                    codeCandidate = c;
                    break;
                }
            }
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
        String expectedKey = (sepayApiAccess != null && !sepayApiAccess.isEmpty()) ? sepayApiAccess : sepayApiKey;
        if (apiKeyHeader == null || expectedKey == null || !expectedKey.equals(apiKeyHeader)) {
            log.warn("Webhook invalid api key id={} acc={} ref={}", payload.getId(), payload.getAccountNumber(), payload.getReferenceCode());
            tx.setStatus(PaymentTransaction.TransactionStatus.invalid);
            transactionRepository.save(tx);
            return;
        }
        if (type != PaymentTransaction.TransferType.in) {
            log.info("Webhook ignored non-in id={} type={} ref={}", payload.getId(), payload.getTransferType(), payload.getReferenceCode());
            tx.setStatus(PaymentTransaction.TransactionStatus.ignored);
            transactionRepository.save(tx);
            return;
        }
        String code = codeCandidate;
        if (code == null || code.isEmpty()) {
            log.warn("Webhook invalid code id={} content={} ref={}", payload.getId(), payload.getContent(), payload.getReferenceCode());
            tx.setStatus(PaymentTransaction.TransactionStatus.invalid);
            transactionRepository.save(tx);
            return;
        }
        Optional<CreditTopupRequest> opt = topupRequestRepository.findByCode(code);
        if (opt.isEmpty()) {
            log.info("Webhook ignored unknown code id={} code={} ref={}", payload.getId(), code, payload.getReferenceCode());
            tx.setStatus(PaymentTransaction.TransactionStatus.ignored);
            transactionRepository.save(tx);
            return;
        }
        CreditTopupRequest req = opt.get();
        if (req.getStatus() == CreditTopupRequest.TopupStatus.paid) {
            tx.setMatchedRequest(req);
            tx.setUser(req.getUser());
            log.info("Webhook ignored already paid id={} code={} ref={}", payload.getId(), code, payload.getReferenceCode());
            tx.setStatus(PaymentTransaction.TransactionStatus.ignored);
            transactionRepository.save(tx);
            return;
        }
        if (!req.getAmount().equals(payload.getTransferAmount())) {
            tx.setMatchedRequest(req);
            tx.setUser(req.getUser());
            log.warn("Webhook invalid amount id={} code={} expected={} actual={} ref={}", payload.getId(), code, req.getAmount(), payload.getTransferAmount(), payload.getReferenceCode());
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
        log.info("Webhook processed id={} code={} amount={} user={} ref={}", payload.getId(), code, payload.getTransferAmount(), user.getId(), payload.getReferenceCode());
    }

    private String generateUniqueCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        } while (topupRequestRepository.findByCode(code).isPresent());
        return code;
    }

    private java.util.List<String> extractCodesFromContent(String content) {
        String cleaned = content == null ? "" : content.toUpperCase();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("[A-Z0-9]{10,16}").matcher(cleaned);
        java.util.List<String> list = new java.util.ArrayList<>();
        while (m.find()) {
            list.add(m.group(0));
        }
        return list;
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
