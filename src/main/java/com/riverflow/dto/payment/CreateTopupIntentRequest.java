package com.riverflow.dto.payment;

import lombok.Data;

@Data
public class CreateTopupIntentRequest {
    private Long amount;
}
