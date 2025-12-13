package com.riverflow.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for admin payment response containing full payment details with user info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPaymentResponse {

    private Long id;

    private Long externalId;

    private String gateway;

    private LocalDateTime transactionDate;

    private String accountNumber;

    private String code;

    private String content;

    private String transferType;

    private Long transferAmount;

    private Long accumulated;

    private String subAccount;

    private String referenceCode;

    private String description;

    private String status;

    private LocalDateTime createdAt;

    // User info (if matched)
    private Long userId;

    private String userEmail;

    private String userFullName;

    // Matched request info
    private Long matchedRequestId;

    private String matchedRequestCode;
}
