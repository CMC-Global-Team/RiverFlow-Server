package com.riverflow.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for currency response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurrencyResponse {
    
    private Integer id;
    private String code;
    private String name;
    private String symbol;
    private Integer decimalPlaces;
    private Boolean isActive;
}

