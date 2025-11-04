package com.riverflow.controller.admin;

import com.riverflow.dto.MessageResponse;
import com.riverflow.dto.admin.FeatureRequest;
import com.riverflow.dto.admin.FeatureResponse;
import com.riverflow.service.admin.FeatureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for admin package feature management
 * All endpoints require ADMIN role
 */
@RestController
@RequestMapping("/admin/features")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class FeatureController {
    
    private final FeatureService featureService;
    
    /**
     * Get all features
     * GET /api/admin/features
     */
    @GetMapping
    public ResponseEntity<List<FeatureResponse>> getAllFeatures(
            @RequestParam(required = false) Boolean isActive
    ) {
        log.info("Admin: Getting all features (isActive: {})", isActive);
        List<FeatureResponse> features = featureService.getAllFeatures(isActive);
        return ResponseEntity.ok(features);
    }
    
    /**
     * Get feature by ID
     * GET /api/admin/features/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<FeatureResponse> getFeatureById(@PathVariable Long id) {
        log.info("Admin: Getting feature by ID: {}", id);
        FeatureResponse response = featureService.getFeatureById(id);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get features by category
     * GET /api/admin/features/category/{category}
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<FeatureResponse>> getFeaturesByCategory(
            @PathVariable String category
    ) {
        log.info("Admin: Getting features by category: {}", category);
        List<FeatureResponse> features = featureService.getFeaturesByCategory(category);
        return ResponseEntity.ok(features);
    }
    
    /**
     * Create new feature
     * POST /api/admin/features
     */
    @PostMapping
    public ResponseEntity<FeatureResponse> createFeature(
            @Valid @RequestBody FeatureRequest request
    ) {
        log.info("Admin: Creating new feature: {}", request.getFeatureName());
        FeatureResponse response = featureService.createFeature(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Update feature
     * PUT /api/admin/features/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<FeatureResponse> updateFeature(
            @PathVariable Long id,
            @Valid @RequestBody FeatureRequest request
    ) {
        log.info("Admin: Updating feature ID: {}", id);
        FeatureResponse response = featureService.updateFeature(id, request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Delete feature
     * DELETE /api/admin/features/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteFeature(@PathVariable Long id) {
        log.info("Admin: Deleting feature ID: {}", id);
        featureService.deleteFeature(id);
        return ResponseEntity.ok(new MessageResponse("Feature deleted successfully"));
    }
}

