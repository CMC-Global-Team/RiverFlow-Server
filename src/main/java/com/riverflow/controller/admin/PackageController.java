package com.riverflow.controller.admin;

import com.riverflow.dto.MessageResponse;
import com.riverflow.dto.admin.*;
import com.riverflow.service.admin.PackageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for admin package management
 * All endpoints require ADMIN role
 */
@RestController
@RequestMapping("/admin/packages")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class PackageController {
    
    private final PackageService packageService;
    
    /**
     * Get all packages
     * GET /api/admin/packages
     */
    @GetMapping
    public ResponseEntity<List<PackageResponse>> getAllPackages(
            @RequestParam(required = false) Boolean isActive
    ) {
        log.info("Admin: Getting all packages (isActive: {})", isActive);
        
        List<PackageResponse> packages;
        if (isActive != null) {
            packages = packageService.getPackagesByStatus(isActive);
        } else {
            packages = packageService.getAllPackages();
        }
        
        return ResponseEntity.ok(packages);
    }
    
    /**
     * Get package by ID
     * GET /api/admin/packages/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<PackageResponse> getPackageById(@PathVariable Long id) {
        log.info("Admin: Getting package by ID: {}", id);
        PackageResponse response = packageService.getPackageById(id);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get package statistics
     * GET /api/admin/packages/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<PackageStatsResponse> getPackageStats() {
        log.info("Admin: Getting package statistics");
        PackageStatsResponse stats = packageService.getPackageStats();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Create new package
     * POST /api/admin/packages
     */
    @PostMapping
    public ResponseEntity<PackageResponse> createPackage(
            @Valid @RequestBody PackageRequest request
    ) {
        log.info("Admin: Creating new package: {}", request.getName());
        PackageResponse response = packageService.createPackage(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Update package
     * PUT /api/admin/packages/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<PackageResponse> updatePackage(
            @PathVariable Long id,
            @Valid @RequestBody PackageRequest request
    ) {
        log.info("Admin: Updating package ID: {}", id);
        PackageResponse response = packageService.updatePackage(id, request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Delete package
     * DELETE /api/admin/packages/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deletePackage(@PathVariable Long id) {
        log.info("Admin: Deleting package ID: {}", id);
        packageService.deletePackage(id);
        return ResponseEntity.ok(new MessageResponse("Package deleted successfully"));
    }
    
    /**
     * Get all available features
     * GET /api/admin/packages/features
     */
    @GetMapping("/features")
    public ResponseEntity<List<FeatureResponse>> getAllFeatures() {
        log.info("Admin: Getting all package features");
        List<FeatureResponse> features = packageService.getAllFeatures();
        return ResponseEntity.ok(features);
    }
    
    /**
     * Get all active currencies
     * GET /api/admin/packages/currencies
     */
    @GetMapping("/currencies")
    public ResponseEntity<List<CurrencyResponse>> getAllCurrencies() {
        log.info("Admin: Getting all active currencies");
        List<CurrencyResponse> currencies = packageService.getAllCurrencies();
        return ResponseEntity.ok(currencies);
    }
}

