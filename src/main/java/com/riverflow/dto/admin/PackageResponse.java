package com.riverflow.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for package response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackageResponse {
    
    private Long id;
    private String name;
    private String description;
    private String slug;
    private BigDecimal basePrice;
    private String baseCurrencyCode;
    private String baseCurrencySymbol;
    private Integer durationDays;
    private Integer maxMindmaps;
    private Integer maxCollaborators;
    private Integer maxStorageMb;
    private Map<String, Object> features;
    private Boolean isActive;
    private Integer displayOrder;
    private Long subscriberCount;
    private List<PriceInfo> prices;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Nested class for price information
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PriceInfo {
        private Long id;
        private String currencyCode;
        private String currencySymbol;
        private String currencyName;
        private BigDecimal price;
        private BigDecimal promotionalPrice;
        private LocalDateTime promotionStartDate;
        private LocalDateTime promotionEndDate;
        private Boolean hasActivePromotion;
    }
}

