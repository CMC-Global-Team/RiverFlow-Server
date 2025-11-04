package com.riverflow.service.admin;

import com.riverflow.dto.admin.FeatureRequest;
import com.riverflow.dto.admin.FeatureResponse;
import com.riverflow.exception.InvalidTokenException;
import com.riverflow.model.PackageFeature;
import com.riverflow.repository.PackageFeatureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for package feature management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureService {
    
    private final PackageFeatureRepository featureRepository;
    
    /**
     * Get all features
     */
    @Transactional(readOnly = true)
    public List<FeatureResponse> getAllFeatures(Boolean isActive) {
        List<PackageFeature> features;
        
        if (isActive != null && isActive) {
            features = featureRepository.findByIsActiveTrue();
        } else {
            features = featureRepository.findAll();
        }
        
        return features.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Get feature by ID
     */
    @Transactional(readOnly = true)
    public FeatureResponse getFeatureById(Long id) {
        PackageFeature feature = featureRepository.findById(id)
                .orElseThrow(() -> new InvalidTokenException("Feature not found with ID: " + id));
        return convertToResponse(feature);
    }
    
    /**
     * Get features by category
     */
    @Transactional(readOnly = true)
    public List<FeatureResponse> getFeaturesByCategory(String category) {
        List<PackageFeature> features = featureRepository.findByCategoryAndIsActiveTrue(category);
        return features.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Create new feature
     */
    @Transactional
    public FeatureResponse createFeature(FeatureRequest request) {
        // Check if feature key already exists
        if (featureRepository.existsByFeatureKey(request.getFeatureKey())) {
            throw new InvalidTokenException("Feature with key '" + request.getFeatureKey() + "' already exists");
        }
        
        PackageFeature feature = PackageFeature.builder()
                .featureKey(request.getFeatureKey())
                .featureName(request.getFeatureName())
                .description(request.getDescription())
                .category(request.getCategory())
                .isActive(request.getIsActive())
                .build();
        
        PackageFeature savedFeature = featureRepository.save(feature);
        log.info("Created feature: {} (ID: {})", savedFeature.getFeatureName(), savedFeature.getId());
        
        return convertToResponse(savedFeature);
    }
    
    /**
     * Update feature
     */
    @Transactional
    public FeatureResponse updateFeature(Long id, FeatureRequest request) {
        PackageFeature feature = featureRepository.findById(id)
                .orElseThrow(() -> new InvalidTokenException("Feature not found with ID: " + id));
        
        // Check if feature key is changed and already exists
        if (!feature.getFeatureKey().equals(request.getFeatureKey()) 
                && featureRepository.existsByFeatureKey(request.getFeatureKey())) {
            throw new InvalidTokenException("Feature with key '" + request.getFeatureKey() + "' already exists");
        }
        
        feature.setFeatureKey(request.getFeatureKey());
        feature.setFeatureName(request.getFeatureName());
        feature.setDescription(request.getDescription());
        feature.setCategory(request.getCategory());
        feature.setIsActive(request.getIsActive());
        
        PackageFeature updatedFeature = featureRepository.save(feature);
        log.info("Updated feature: {} (ID: {})", updatedFeature.getFeatureName(), id);
        
        return convertToResponse(updatedFeature);
    }
    
    /**
     * Delete feature
     */
    @Transactional
    public void deleteFeature(Long id) {
        PackageFeature feature = featureRepository.findById(id)
                .orElseThrow(() -> new InvalidTokenException("Feature not found with ID: " + id));
        
        featureRepository.delete(feature);
        log.info("Deleted feature: {} (ID: {})", feature.getFeatureName(), id);
    }
    
    /**
     * Convert PackageFeature to FeatureResponse
     */
    private FeatureResponse convertToResponse(PackageFeature feature) {
        return FeatureResponse.builder()
                .id(feature.getId())
                .featureKey(feature.getFeatureKey())
                .featureName(feature.getFeatureName())
                .description(feature.getDescription())
                .category(feature.getCategory())
                .isActive(feature.getIsActive())
                .createdAt(feature.getCreatedAt())
                .build();
    }
}

