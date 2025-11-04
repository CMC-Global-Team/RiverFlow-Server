package com.riverflow.dto.admin;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating/updating currencies
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurrencyRequest {
    
    @NotBlank(message = "Currency code is required")
    @Size(min = 3, max = 3, message = "Currency code must be exactly 3 characters")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency code must be 3 uppercase letters (e.g., USD, EUR)")
    private String code;
    
    @NotBlank(message = "Currency name is required")
    @Size(max = 100, message = "Currency name must not exceed 100 characters")
    private String name;
    
    @NotBlank(message = "Currency symbol is required")
    @Size(max = 10, message = "Currency symbol must not exceed 10 characters")
    private String symbol;
    
    @NotNull(message = "Decimal places is required")
    @Min(value = 0, message = "Decimal places must be non-negative")
    @Max(value = 4, message = "Decimal places must not exceed 4")
    private Integer decimalPlaces = 2;
    
    private Boolean isActive = true;
    
    private Integer displayOrder = 0;
}

