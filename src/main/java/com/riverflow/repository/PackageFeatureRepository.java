package com.riverflow.repository;

import com.riverflow.model.PackageFeature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for PackageFeature entity
 */
@Repository
public interface PackageFeatureRepository extends JpaRepository<PackageFeature, Long> {
    
    /**
     * Find feature by feature key
     */
    Optional<PackageFeature> findByFeatureKey(String featureKey);
    
    /**
     * Find all active features
     */
    List<PackageFeature> findByIsActiveTrue();
    
    /**
     * Find features by category
     */
    List<PackageFeature> findByCategoryAndIsActiveTrue(String category);
    
    /**
     * Check if feature exists by key
     */
    boolean existsByFeatureKey(String featureKey);
}

