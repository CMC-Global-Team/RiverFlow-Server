package com.riverflow.controller.admin;

import com.riverflow.dto.admin.AdminDashboardResponse;
import com.riverflow.model.User;
import com.riverflow.service.admin.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for Admin Dashboard endpoints
 * Accessible by both ADMIN and SUPER_ADMIN roles
 */
@Slf4j
@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    /**
     * Get dashboard data
     * Returns overview stats, quick stats, and recent activity (SUPER_ADMIN only)
     * GET /api/admin/dashboard
     */
    @GetMapping
    public ResponseEntity<AdminDashboardResponse> getDashboard(
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Fetching dashboard data for user: {}", userDetails.getUsername());

        // Determine user role from authorities
        User.Role userRole = determineUserRole(userDetails);

        AdminDashboardResponse dashboard = adminDashboardService.getDashboardData(userRole);
        return ResponseEntity.ok(dashboard);
    }

    /**
     * Determine user role from Spring Security authorities
     */
    private User.Role determineUserRole(UserDetails userDetails) {
        if (userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"))) {
            return User.Role.super_admin;
        }
        return User.Role.admin;
    }
}
