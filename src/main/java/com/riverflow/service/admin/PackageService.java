package com.riverflow.service.admin;

import com.riverflow.dto.admin.*;
import com.riverflow.exception.InvalidTokenException;
import com.riverflow.model.*;
import com.riverflow.model.Package;
import com.riverflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for package management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PackageService {
    
    private final PackageRepository packageRepository;
    private final PackageFeatureRepository packageFeatureRepository;
    private final PackagePriceRepository packagePriceRepository;
    private final CurrencyRepository currencyRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    
    /**
     * Get all packages
     */
    @Transactional(readOnly = true)
    public List<PackageResponse> getAllPackages() {
        List<Package> packages = packageRepository.findAllByOrderByDisplayOrderAsc();
        return packages.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Get packages by status
     */
    @Transactional(readOnly = true)
    public List<PackageResponse> getPackagesByStatus(Boolean isActive) {
        List<Package> packages = packageRepository.findByIsActiveOrderByDisplayOrderAsc(isActive);
        return packages.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Get package by ID
     */
    @Transactional(readOnly = true)
    public PackageResponse getPackageById(Long id) {
        Package pkg = packageRepository.findById(id)
                .orElseThrow(() -> new InvalidTokenException("Package not found with ID: " + id));
        return convertToResponse(pkg);
    }
    
    /**
     * Get package statistics
     */
    @Transactional(readOnly = true)
    public PackageStatsResponse getPackageStats() {
        long totalPackages = packageRepository.count();
        long activePackages = packageRepository.countByIsActiveTrue();
        long activeSubscribers = userSubscriptionRepository.countActiveSubscriptions();
        
        // Calculate MRR (simplified calculation)
        BigDecimal mrr = calculateMRR();
        
        return PackageStatsResponse.builder()
                .totalPackages(totalPackages)
                .activePackages(activePackages)
                .activeSubscribers(activeSubscribers)
                .monthlyRecurringRevenue(mrr)
                .conversionRate(23.5) // Mock data - should be calculated
                .growthPercentage(12.5) // Mock data - should be calculated from historical data
                .build();
    }
    
    /**
     * Create new package
     */
    @Transactional
    public PackageResponse createPackage(PackageRequest request) {
        // Check if slug already exists
        if (packageRepository.existsBySlug(request.getSlug())) {
            throw new InvalidTokenException("Package with slug '" + request.getSlug() + "' already exists");
        }
        
        // Get base currency
        Currency baseCurrency = currencyRepository.findByCode(request.getBaseCurrencyCode())
                .orElseThrow(() -> new InvalidTokenException("Currency not found: " + request.getBaseCurrencyCode()));
        
        // Create package
        Package pkg = Package.builder()
                .name(request.getName())
                .description(request.getDescription())
                .slug(request.getSlug())
                .basePrice(request.getBasePrice())
                .baseCurrency(baseCurrency)
                .durationDays(request.getDurationDays())
                .maxMindmaps(request.getMaxMindmaps())
                .maxCollaborators(request.getMaxCollaborators())
                .maxStorageMb(request.getMaxStorageMb())
                .features(request.getFeatures())
                .isActive(request.getIsActive())
                .displayOrder(request.getDisplayOrder())
                .build();
        
        Package savedPackage = packageRepository.save(pkg);
        log.info("Created package: {} (ID: {})", savedPackage.getName(), savedPackage.getId());
        
        // Create prices for different currencies
        if (request.getPrices() != null && !request.getPrices().isEmpty()) {
            createPackagePrices(savedPackage, request.getPrices());
        }
        
        return convertToResponse(savedPackage);
    }
    
    /**
     * Update package
     */
    @Transactional
    public PackageResponse updatePackage(Long id, PackageRequest request) {
        Package pkg = packageRepository.findById(id)
                .orElseThrow(() -> new InvalidTokenException("Package not found with ID: " + id));
        
        // Check if slug is changed and already exists
        if (!pkg.getSlug().equals(request.getSlug()) && packageRepository.existsBySlug(request.getSlug())) {
            throw new InvalidTokenException("Package with slug '" + request.getSlug() + "' already exists");
        }
        
        // Get base currency
        Currency baseCurrency = currencyRepository.findByCode(request.getBaseCurrencyCode())
                .orElseThrow(() -> new InvalidTokenException("Currency not found: " + request.getBaseCurrencyCode()));
        
        // Update package fields
        pkg.setName(request.getName());
        pkg.setDescription(request.getDescription());
        pkg.setSlug(request.getSlug());
        pkg.setBasePrice(request.getBasePrice());
        pkg.setBaseCurrency(baseCurrency);
        pkg.setDurationDays(request.getDurationDays());
        pkg.setMaxMindmaps(request.getMaxMindmaps());
        pkg.setMaxCollaborators(request.getMaxCollaborators());
        pkg.setMaxStorageMb(request.getMaxStorageMb());
        pkg.setFeatures(request.getFeatures());
        pkg.setIsActive(request.getIsActive());
        pkg.setDisplayOrder(request.getDisplayOrder());
        
        Package updatedPackage = packageRepository.save(pkg);
        log.info("Updated package: {} (ID: {})", updatedPackage.getName(), updatedPackage.getId());
        
        // Update prices
        if (request.getPrices() != null) {
            // Delete existing prices and create new ones
            packagePriceRepository.deleteByPackageEntityId(id);
            createPackagePrices(updatedPackage, request.getPrices());
        }
        
        return convertToResponse(updatedPackage);
    }
    
    /**
     * Delete package
     */
    @Transactional
    public void deletePackage(Long id) {
        Package pkg = packageRepository.findById(id)
                .orElseThrow(() -> new InvalidTokenException("Package not found with ID: " + id));
        
        // Check if package has active subscriptions
        long activeSubscriptions = userSubscriptionRepository.countActiveSubscriptionsByPackageId(id);
        if (activeSubscriptions > 0) {
            throw new InvalidTokenException("Cannot delete package with active subscriptions. Please deactivate it instead.");
        }
        
        packageRepository.delete(pkg);
        log.info("Deleted package: {} (ID: {})", pkg.getName(), id);
    }
    
    /**
     * Get all package features
     */
    @Transactional(readOnly = true)
    public List<FeatureResponse> getAllFeatures() {
        List<PackageFeature> features = packageFeatureRepository.findByIsActiveTrue();
        return features.stream()
                .map(this::convertToFeatureResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all active currencies
     */
    @Transactional(readOnly = true)
    public List<CurrencyResponse> getAllCurrencies() {
        List<Currency> currencies = currencyRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        return currencies.stream()
                .map(this::convertToCurrencyResponse)
                .collect(Collectors.toList());
    }
    
    // ========== Private Helper Methods ==========
    
    /**
     * Convert Package entity to PackageResponse DTO
     */
    private PackageResponse convertToResponse(Package pkg) {
        // Get subscriber count
        long subscriberCount = userSubscriptionRepository.countActiveSubscriptionsByPackageId(pkg.getId());
        
        // Get prices
        List<PackagePrice> prices = packagePriceRepository.findByPackageEntityId(pkg.getId());
        List<PackageResponse.PriceInfo> priceInfos = prices.stream()
                .map(this::convertToPriceInfo)
                .collect(Collectors.toList());
        
        return PackageResponse.builder()
                .id(pkg.getId())
                .name(pkg.getName())
                .description(pkg.getDescription())
                .slug(pkg.getSlug())
                .basePrice(pkg.getBasePrice())
                .baseCurrencyCode(pkg.getBaseCurrency().getCode())
                .baseCurrencySymbol(pkg.getBaseCurrency().getSymbol())
                .durationDays(pkg.getDurationDays())
                .maxMindmaps(pkg.getMaxMindmaps())
                .maxCollaborators(pkg.getMaxCollaborators())
                .maxStorageMb(pkg.getMaxStorageMb())
                .features(pkg.getFeatures())
                .isActive(pkg.getIsActive())
                .displayOrder(pkg.getDisplayOrder())
                .subscriberCount(subscriberCount)
                .prices(priceInfos)
                .createdAt(pkg.getCreatedAt())
                .updatedAt(pkg.getUpdatedAt())
                .build();
    }
    
    /**
     * Convert PackagePrice to PriceInfo
     */
    private PackageResponse.PriceInfo convertToPriceInfo(PackagePrice price) {
        LocalDateTime now = LocalDateTime.now();
        boolean hasActivePromotion = price.getPromotionalPrice() != null
                && (price.getPromotionStartDate() == null || price.getPromotionStartDate().isBefore(now))
                && (price.getPromotionEndDate() == null || price.getPromotionEndDate().isAfter(now));
        
        return PackageResponse.PriceInfo.builder()
                .id(price.getId())
                .currencyCode(price.getCurrency().getCode())
                .currencySymbol(price.getCurrency().getSymbol())
                .currencyName(price.getCurrency().getName())
                .price(price.getPrice())
                .promotionalPrice(price.getPromotionalPrice())
                .promotionStartDate(price.getPromotionStartDate())
                .promotionEndDate(price.getPromotionEndDate())
                .hasActivePromotion(hasActivePromotion)
                .build();
    }
    
    /**
     * Convert PackageFeature to FeatureResponse
     */
    private FeatureResponse convertToFeatureResponse(PackageFeature feature) {
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
    
    /**
     * Convert Currency to CurrencyResponse
     */
    private CurrencyResponse convertToCurrencyResponse(Currency currency) {
        return CurrencyResponse.builder()
                .id(currency.getId())
                .code(currency.getCode())
                .name(currency.getName())
                .symbol(currency.getSymbol())
                .decimalPlaces(currency.getDecimalPlaces())
                .isActive(currency.getIsActive())
                .build();
    }
    
    /**
     * Create package prices for different currencies
     */
    private void createPackagePrices(Package pkg, List<PackageRequest.PriceData> pricesData) {
        for (PackageRequest.PriceData priceData : pricesData) {
            Currency currency = currencyRepository.findByCode(priceData.getCurrencyCode())
                    .orElseThrow(() -> new InvalidTokenException("Currency not found: " + priceData.getCurrencyCode()));
            
            PackagePrice packagePrice = PackagePrice.builder()
                    .packageEntity(pkg)
                    .currency(currency)
                    .price(priceData.getPrice())
                    .promotionalPrice(priceData.getPromotionalPrice())
                    .promotionStartDate(parseDateTime(priceData.getPromotionStartDate()))
                    .promotionEndDate(parseDateTime(priceData.getPromotionEndDate()))
                    .isActive(true)
                    .build();
            
            packagePriceRepository.save(packagePrice);
        }
    }
    
    /**
     * Parse ISO date string to LocalDateTime
     */
    private LocalDateTime parseDateTime(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            log.warn("Failed to parse date: {}", dateString);
            return null;
        }
    }
    
    /**
     * Calculate Monthly Recurring Revenue
     */
    private BigDecimal calculateMRR() {
        // Simplified calculation - in real implementation, this should:
        // 1. Get all active subscriptions
        // 2. Normalize to monthly amount (30 days)
        // 3. Sum all amounts
        // For now, return mock data
        return BigDecimal.valueOf(45231.00);
    }
}

