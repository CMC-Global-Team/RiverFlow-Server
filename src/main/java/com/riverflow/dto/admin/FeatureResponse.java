package com.riverflow.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for package feature response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureResponse {
    
    private Long id;
    private String featureKey;
    private String featureName;
    private String description;
    private String category;
    private Boolean isActive;
    private LocalDateTime createdAt;
}

