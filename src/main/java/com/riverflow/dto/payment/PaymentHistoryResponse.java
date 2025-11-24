package com.riverflow.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentHistoryResponse {

    private Long id;

    private String transactionCode;

    private Long amount;

    private String status;

    private LocalDateTime date;

    private String gateway;

    private String content;
}