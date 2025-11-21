package com.riverflow.model.payment;

import com.riverflow.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions", indexes = {
        @Index(name = "idx_gateway_date", columnList = "gateway, transaction_date"),
        @Index(name = "idx_code", columnList = "code"),
        @Index(name = "idx_user", columnList = "user_id"),
        @Index(name = "idx_matched_request", columnList = "matched_request_id"),
        @Index(name = "idx_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id")
    private Long externalId;

    @Column(name = "gateway", nullable = false, length = 50)
    private String gateway;

    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;

    @Column(name = "account_number", length = 32)
    private String accountNumber;

    @Column(name = "code", length = 64)
    private String code;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "transfer_type", nullable = false, length = 10)
    private TransferType transferType;

    @Column(name = "transfer_amount", nullable = false)
    private Long transferAmount;

    @Column(name = "accumulated")
    private Long accumulated;

    @Column(name = "sub_account", length = 64)
    private String subAccount;

    @Column(name = "reference_code", length = 64)
    private String referenceCode;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "matched_request_id")
    private CreditTopupRequest matchedRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status = TransactionStatus.pending;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum TransferType {
        in,
        out
    }

    public enum TransactionStatus {
        pending,
        matched,
        processed,
        ignored,
        invalid
    }
}
