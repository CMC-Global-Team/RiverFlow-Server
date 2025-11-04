package com.riverflow.repository;

import com.riverflow.model.Package;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Package entity
 */
@Repository
public interface PackageRepository extends JpaRepository<Package, Long> {
    
    /**
     * Find package by slug
     */
    Optional<Package> findBySlug(String slug);
    
    /**
     * Check if package exists by slug
     */
    boolean existsBySlug(String slug);
    
    /**
     * Find all active packages ordered by display order
     */
    List<Package> findByIsActiveTrueOrderByDisplayOrderAsc();
    
    /**
     * Find all packages ordered by display order
     */
    List<Package> findAllByOrderByDisplayOrderAsc();
    
    /**
     * Count active packages
     */
    long countByIsActiveTrue();
    
    /**
     * Find packages by active status
     */
    List<Package> findByIsActiveOrderByDisplayOrderAsc(Boolean isActive);
}

