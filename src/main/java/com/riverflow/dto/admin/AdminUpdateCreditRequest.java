package com.riverflow.dto.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating user credit
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUpdateCreditRequest {

    @NotNull(message = "Credit amount is required")
    @Min(value = 0, message = "Credit cannot be negative")
    private Long credit;
}
