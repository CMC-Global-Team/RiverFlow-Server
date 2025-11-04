package com.riverflow.repository;

import com.riverflow.model.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Currency entity
 */
@Repository
public interface CurrencyRepository extends JpaRepository<Currency, Integer> {
    
    /**
     * Find currency by code (e.g., "USD", "EUR")
     */
    Optional<Currency> findByCode(String code);
    
    /**
     * Find all active currencies ordered by display order
     */
    List<Currency> findByIsActiveTrueOrderByDisplayOrderAsc();
    
    /**
     * Check if currency exists by code
     */
    boolean existsByCode(String code);
}

