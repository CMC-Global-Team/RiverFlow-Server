package com.riverflow.controller.admin;

import com.riverflow.dto.MessageResponse;
import com.riverflow.dto.admin.CurrencyRequest;
import com.riverflow.dto.admin.CurrencyResponse;
import com.riverflow.service.admin.CurrencyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for admin currency management
 * All endpoints require ADMIN role
 */
@RestController
@RequestMapping("/admin/currencies")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class CurrencyController {
    
    private final CurrencyService currencyService;
    
    /**
     * Get all currencies
     * GET /api/admin/currencies
     */
    @GetMapping
    public ResponseEntity<List<CurrencyResponse>> getAllCurrencies(
            @RequestParam(required = false) Boolean isActive
    ) {
        log.info("Admin: Getting all currencies (isActive: {})", isActive);
        List<CurrencyResponse> currencies = currencyService.getAllCurrencies(isActive);
        return ResponseEntity.ok(currencies);
    }
    
    /**
     * Get currency by ID
     * GET /api/admin/currencies/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<CurrencyResponse> getCurrencyById(@PathVariable Integer id) {
        log.info("Admin: Getting currency by ID: {}", id);
        CurrencyResponse response = currencyService.getCurrencyById(id);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Create new currency
     * POST /api/admin/currencies
     */
    @PostMapping
    public ResponseEntity<CurrencyResponse> createCurrency(
            @Valid @RequestBody CurrencyRequest request
    ) {
        log.info("Admin: Creating new currency: {}", request.getCode());
        CurrencyResponse response = currencyService.createCurrency(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Update currency
     * PUT /api/admin/currencies/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<CurrencyResponse> updateCurrency(
            @PathVariable Integer id,
            @Valid @RequestBody CurrencyRequest request
    ) {
        log.info("Admin: Updating currency ID: {}", id);
        CurrencyResponse response = currencyService.updateCurrency(id, request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Delete currency
     * DELETE /api/admin/currencies/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteCurrency(@PathVariable Integer id) {
        log.info("Admin: Deleting currency ID: {}", id);
        currencyService.deleteCurrency(id);
        return ResponseEntity.ok(new MessageResponse("Currency deleted successfully"));
    }
}

