package com.riverflow.repository;

import com.riverflow.model.PackagePrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for PackagePrice entity
 */
@Repository
public interface PackagePriceRepository extends JpaRepository<PackagePrice, Long> {
    
    /**
     * Find all prices for a specific package
     */
    List<PackagePrice> findByPackageEntityId(Long packageId);
    
    /**
     * Find price for a package in a specific currency
     */
    Optional<PackagePrice> findByPackageEntityIdAndCurrencyId(Long packageId, Integer currencyId);
    
    /**
     * Delete all prices for a package
     */
    void deleteByPackageEntityId(Long packageId);
    
    /**
     * Find active prices for a package
     */
    List<PackagePrice> findByPackageEntityIdAndIsActiveTrue(Long packageId);
}

