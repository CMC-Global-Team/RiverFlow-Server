package com.riverflow.controller.admin;

import com.riverflow.dto.admin.AdminChangePasswordRequest;
import com.riverflow.dto.admin.AdminUpdateCreditRequest;
import com.riverflow.dto.admin.AdminUserRequest;
import com.riverflow.dto.admin.AdminUserResponse;
import com.riverflow.dto.payment.PaymentHistoryResponse;
import com.riverflow.service.admin.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for admin user management
 * All endpoints require ADMIN role
 */
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * Get all users with pagination, search, filter, and sort
     * GET
     * /api/admin/users?page=0&size=10&search=email&status=active&role=user&sortBy=createdAt&sortDir=desc
     */
    @GetMapping
    public ResponseEntity<Page<AdminUserResponse>> getAllUsers(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<AdminUserResponse> users = adminUserService.getAllUsers(
                search, status, role, sortBy, sortDir, page, size);
        return ResponseEntity.ok(users);
    }

    /**
     * Get user by ID
     * GET /api/admin/users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<AdminUserResponse> getUserById(@PathVariable Long id) {
        AdminUserResponse user = adminUserService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    /**
     * Update user information
     * PUT /api/admin/users/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<AdminUserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody AdminUserRequest request) {
        AdminUserResponse user = adminUserService.updateUser(id, request);
        return ResponseEntity.ok(user);
    }

    /**
     * Soft delete user (set status to 'deleted')
     * DELETE /api/admin/users/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long id) {
        adminUserService.softDeleteUser(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "User deleted successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Update user credit
     * PUT /api/admin/users/{id}/credit
     */
    @PutMapping("/{id}/credit")
    public ResponseEntity<AdminUserResponse> updateUserCredit(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateCreditRequest request) {
        AdminUserResponse user = adminUserService.updateUserCredit(id, request);
        return ResponseEntity.ok(user);
    }

    /**
     * Change user password
     * PUT /api/admin/users/{id}/password
     */
    @PutMapping("/{id}/password")
    public ResponseEntity<Map<String, String>> changeUserPassword(
            @PathVariable Long id,
            @Valid @RequestBody AdminChangePasswordRequest request) {
        adminUserService.changeUserPassword(id, request);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Password changed successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Get user payment history
     * GET /api/admin/users/{id}/payments?page=0&size=10
     */
    @GetMapping("/{id}/payments")
    public ResponseEntity<Page<PaymentHistoryResponse>> getUserPaymentHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<PaymentHistoryResponse> history = adminUserService.getUserPaymentHistory(id, page, size);
        return ResponseEntity.ok(history);
    }
}
