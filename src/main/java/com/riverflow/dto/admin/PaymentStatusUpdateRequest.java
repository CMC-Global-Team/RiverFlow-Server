package com.riverflow.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating payment status
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusUpdateRequest {

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(pending|matched|processed|ignored|invalid)$", message = "Status must be one of: pending, matched, processed, ignored, invalid")
    private String status;
}
