package com.riverflow.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating/updating package features
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureRequest {
    
    @NotBlank(message = "Feature key is required")
    @Size(max = 100, message = "Feature key must not exceed 100 characters")
    private String featureKey;
    
    @NotBlank(message = "Feature name is required")
    @Size(max = 255, message = "Feature name must not exceed 255 characters")
    private String featureName;
    
    private String description;
    
    @NotBlank(message = "Category is required")
    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;
    
    private Boolean isActive = true;
}

