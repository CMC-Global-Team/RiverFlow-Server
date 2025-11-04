package com.riverflow.dto.admin;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * DTO for creating/updating packages
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackageRequest {
    
    @NotBlank(message = "Package name is required")
    @Size(max = 100, message = "Package name must not exceed 100 characters")
    private String name;
    
    private String description;
    
    @NotBlank(message = "Slug is required")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must contain only lowercase letters, numbers, and hyphens")
    @Size(max = 100, message = "Slug must not exceed 100 characters")
    private String slug;
    
    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.0", message = "Base price must be non-negative")
    private BigDecimal basePrice;
    
    @NotNull(message = "Base currency code is required")
    private String baseCurrencyCode; // e.g., "USD"
    
    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 day")
    private Integer durationDays;
    
    @NotNull(message = "Max mindmaps is required")
    @Min(value = 0, message = "Max mindmaps must be non-negative")
    private Integer maxMindmaps;
    
    @NotNull(message = "Max collaborators is required")
    @Min(value = 0, message = "Max collaborators must be non-negative")
    private Integer maxCollaborators;
    
    @NotNull(message = "Max storage is required")
    @Min(value = 0, message = "Max storage must be non-negative")
    private Integer maxStorageMb;
    
    /**
     * Package features as key-value pairs
     * Example: {"real_time_collaboration": true, "export_pdf": true}
     */
    private Map<String, Object> features;
    
    /**
     * Prices in different currencies
     */
    private List<PriceData> prices;
    
    private Boolean isActive = true;
    
    private Integer displayOrder = 0;
    
    /**
     * Nested class for price data
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PriceData {
        
        @NotNull(message = "Currency code is required")
        private String currencyCode;
        
        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", message = "Price must be non-negative")
        private BigDecimal price;
        
        @DecimalMin(value = "0.0", message = "Promotional price must be non-negative")
        private BigDecimal promotionalPrice;
        
        private String promotionStartDate; // ISO format
        
        private String promotionEndDate; // ISO format
    }
}

