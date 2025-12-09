package com.riverflow.service.payment;

import com.riverflow.dto.payment.SepayWebhookPayload;
import com.riverflow.model.User;
import com.riverflow.model.payment.CreditTopupRequest;
import com.riverflow.model.payment.PaymentTransaction;
import com.riverflow.repository.UserRepository;
import com.riverflow.repository.payment.CreditTopupRequestRepository;
import com.riverflow.repository.payment.PaymentTransactionRepository;
import com.riverflow.service.logging.ActivityLoggingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import com.riverflow.dto.payment.PaymentHistoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final CreditTopupRequestRepository topupRequestRepository;
    private final UserRepository userRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final ActivityLoggingService activityLoggingService;

    @Value("${app.sepay.api-key:}")
    private String sepayApiKey;

    @Value("${app.sepay.api-access:}")
    private String sepayApiAccess;

    @Value("${app.sepay.account-number}")
    private String accountNumber;

    @Value("${app.sepay.bank}")
    private String bank;

    @Value("${app.sepay.require-auth:true}")
    private boolean sepayRequireAuth;

    @Value("${app.credit.rate-vnd-per-credit:1000}")
    private long vndPerCredit;

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
        return base + "?acc=" + accountNumber + "&bank=" + bank + "&amount=" + amount + "&des=" + code + "&template="
                + template + "&download=" + download;
    }

    @Transactional
    public void handleSepayWebhook(SepayWebhookPayload payload, String apiKeyHeader) {
        if (apiKeyHeader != null) {
            apiKeyHeader = apiKeyHeader.trim();
            if ((apiKeyHeader.startsWith("\"") && apiKeyHeader.endsWith("\""))
                    || (apiKeyHeader.startsWith("'") && apiKeyHeader.endsWith("'"))) {
                apiKeyHeader = apiKeyHeader.substring(1, apiKeyHeader.length() - 1);
            }
        }
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
        PaymentTransaction.TransferType type = payload.getTransferType() != null
                && payload.getTransferType().equalsIgnoreCase("in")
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
        if (expectedKey != null) {
            expectedKey = expectedKey.trim();
        }
        boolean keyOk = expectedKey != null && apiKeyHeader != null && expectedKey.equals(apiKeyHeader);
        if (sepayRequireAuth && !keyOk) {
            Integer expLen = expectedKey == null ? null : expectedKey.length();
            Integer hdrLen = apiKeyHeader == null ? null : apiKeyHeader.length();
            System.out.println("[PAYMENT DEBUG] Auth validation failed - requireAuth: " + sepayRequireAuth + ", keyOk: "
                    + keyOk + ", expLen: " + expLen + ", hdrLen: " + hdrLen);
            tx.setStatus(PaymentTransaction.TransactionStatus.invalid);
            transactionRepository.save(tx);
            return;
        }
        if (payload.getAccountNumber() == null || !payload.getAccountNumber().equals(accountNumber)) {
            System.out.println("[PAYMENT DEBUG] Account number mismatch - payload: " + payload.getAccountNumber()
                    + ", expected: " + accountNumber);
            tx.setStatus(PaymentTransaction.TransactionStatus.ignored);
            transactionRepository.save(tx);
            return;
        }
        if (type != PaymentTransaction.TransferType.in) {
            tx.setStatus(PaymentTransaction.TransactionStatus.ignored);
            transactionRepository.save(tx);
            return;
        }
        String code = codeCandidate;
        if (code == null || code.isEmpty()) {
            System.out.println("[PAYMENT DEBUG] Code extraction failed - codeCandidate: " + codeCandidate
                    + ", content: " + payload.getContent());
            tx.setStatus(PaymentTransaction.TransactionStatus.invalid);
            transactionRepository.save(tx);
            return;
        }
        Optional<CreditTopupRequest> opt = topupRequestRepository.findByCode(code);
        if (opt.isEmpty()) {
            System.out.println("[PAYMENT DEBUG] Topup request not found - code: " + code);
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
            System.out.println("[PAYMENT DEBUG] Amount mismatch - request amount: " + req.getAmount()
                    + ", transfer amount: " + payload.getTransferAmount());
            tx.setMatchedRequest(req);
            tx.setUser(req.getUser());
            tx.setStatus(PaymentTransaction.TransactionStatus.invalid);
            transactionRepository.save(tx);
            return;
        }
        User user = req.getUser();
        long current = user.getCredit() == null ? 0L : user.getCredit();
        long addCredits = vndPerCredit > 0 ? (payload.getTransferAmount() / vndPerCredit) : 0L;
        System.out.println("[PAYMENT DEBUG] SUCCESS - Processing payment for user " + user.getId() + ", adding "
                + addCredits + " credits (current: " + current + ")");
        user.setCredit(current + addCredits);
        req.setStatus(CreditTopupRequest.TopupStatus.paid);
        req.setPaidAt(LocalDateTime.now());
        userRepository.save(user);
        topupRequestRepository.save(req);
        tx.setMatchedRequest(req);
        tx.setUser(user);
        tx.setStatus(PaymentTransaction.TransactionStatus.processed);
        transactionRepository.save(tx);

        // Log payment success to activity logs
        activityLoggingService.logPaymentSuccess(
                user.getId(),
                user.getEmail(),
                tx.getId(),
                payload.getTransferAmount(),
                addCredits);
    }

    private String generateUniqueCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        } while (topupRequestRepository.findByCode(code).isPresent());
        return code;
    }

    private java.util.List<String> extractCodesFromContent(String content) {
        java.util.List<String> candidates = new java.util.ArrayList<>();
        if (content == null || content.isEmpty()) {
            return candidates;
        }

        String cleaned = content.toUpperCase();

        // Split by common delimiters: hyphen, space, underscore, etc.
        String[] tokens = cleaned.split("[\\s\\-_,;:|/\\\\]+");

        // Extract all alphanumeric tokens that match our code length (10-16 characters)
        for (String token : tokens) {
            // Remove any non-alphanumeric characters from token
            String alphanumeric = token.replaceAll("[^A-Z0-9]", "");

            if (alphanumeric.length() >= 10 && alphanumeric.length() <= 16) {
                candidates.add(alphanumeric);
            }
        }

        // Also try removing ALL non-alphanumeric and extracting patterns
        // This handles cases where code might be embedded without clear delimiters
        String fullyClean = cleaned.replaceAll("[^A-Z0-9]", "");
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("[A-Z0-9]{10,16}").matcher(fullyClean);
        while (m.find()) {
            String candidate = m.group(0);
            if (!candidates.contains(candidate)) {
                candidates.add(candidate);
            }
        }

        return candidates;
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

    @Transactional(readOnly = true)
    public Page<PaymentHistoryResponse> getUserTransactions(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<PaymentTransaction> transactionPage = transactionRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable);

        return transactionPage.map(this::mapToHistoryResponse);
    }

    private PaymentHistoryResponse mapToHistoryResponse(PaymentTransaction entity) {
        return PaymentHistoryResponse.builder()
                .id(entity.getId())
                .transactionCode(entity.getCode())
                .amount(entity.getTransferAmount())
                .status(entity.getStatus() != null ? entity.getStatus().name() : "unknown")
                .date(entity.getCreatedAt())
                .gateway(entity.getGateway())
                .content(entity.getContent())
                .build();
    }
}
