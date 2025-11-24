package com.riverflow.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TopupIntentResponse {
    private String code;
    private Long amount;
    private String qrUrl;
}
