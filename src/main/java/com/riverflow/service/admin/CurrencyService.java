package com.riverflow.service.admin;

import com.riverflow.dto.admin.CurrencyRequest;
import com.riverflow.dto.admin.CurrencyResponse;
import com.riverflow.exception.InvalidTokenException;
import com.riverflow.model.Currency;
import com.riverflow.repository.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for currency management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CurrencyService {
    
    private final CurrencyRepository currencyRepository;
    
    /**
     * Get all currencies
     */
    @Transactional(readOnly = true)
    public List<CurrencyResponse> getAllCurrencies(Boolean isActive) {
        List<Currency> currencies;
        
        if (isActive != null && isActive) {
            currencies = currencyRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        } else {
            currencies = currencyRepository.findAll();
        }
        
        return currencies.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Get currency by ID
     */
    @Transactional(readOnly = true)
    public CurrencyResponse getCurrencyById(Integer id) {
        Currency currency = currencyRepository.findById(id)
                .orElseThrow(() -> new InvalidTokenException("Currency not found with ID: " + id));
        return convertToResponse(currency);
    }
    
    /**
     * Create new currency
     */
    @Transactional
    public CurrencyResponse createCurrency(CurrencyRequest request) {
        // Check if currency code already exists
        if (currencyRepository.existsByCode(request.getCode())) {
            throw new InvalidTokenException("Currency with code '" + request.getCode() + "' already exists");
        }
        
        Currency currency = Currency.builder()
                .code(request.getCode().toUpperCase())
                .name(request.getName())
                .symbol(request.getSymbol())
                .decimalPlaces(request.getDecimalPlaces().byteValue())
                .isActive(request.getIsActive())
                .displayOrder(request.getDisplayOrder())
                .build();
        
        Currency savedCurrency = currencyRepository.save(currency);
        log.info("Created currency: {} ({})", savedCurrency.getName(), savedCurrency.getCode());
        
        return convertToResponse(savedCurrency);
    }
    
    /**
     * Update currency
     */
    @Transactional
    public CurrencyResponse updateCurrency(Integer id, CurrencyRequest request) {
        Currency currency = currencyRepository.findById(id)
                .orElseThrow(() -> new InvalidTokenException("Currency not found with ID: " + id));
        
        // Check if currency code is changed and already exists
        if (!currency.getCode().equals(request.getCode()) 
                && currencyRepository.existsByCode(request.getCode())) {
            throw new InvalidTokenException("Currency with code '" + request.getCode() + "' already exists");
        }
        
        currency.setCode(request.getCode().toUpperCase());
        currency.setName(request.getName());
        currency.setSymbol(request.getSymbol());
        currency.setDecimalPlaces(request.getDecimalPlaces().byteValue());
        currency.setIsActive(request.getIsActive());
        currency.setDisplayOrder(request.getDisplayOrder());
        
        Currency updatedCurrency = currencyRepository.save(currency);
        log.info("Updated currency: {} ({})", updatedCurrency.getName(), updatedCurrency.getCode());
        
        return convertToResponse(updatedCurrency);
    }
    
    /**
     * Delete currency
     */
    @Transactional
    public void deleteCurrency(Integer id) {
        Currency currency = currencyRepository.findById(id)
                .orElseThrow(() -> new InvalidTokenException("Currency not found with ID: " + id));
        
        // TODO: Check if currency is being used in packages or prices
        
        currencyRepository.delete(currency);
        log.info("Deleted currency: {} ({})", currency.getName(), currency.getCode());
    }
    
    /**
     * Convert Currency to CurrencyResponse
     */
    private CurrencyResponse convertToResponse(Currency currency) {
        return CurrencyResponse.builder()
                .id(currency.getId())
                .code(currency.getCode())
                .name(currency.getName())
                .symbol(currency.getSymbol())
                .decimalPlaces(currency.getDecimalPlaces().intValue())
                .isActive(currency.getIsActive())
                .build();
    }
}

