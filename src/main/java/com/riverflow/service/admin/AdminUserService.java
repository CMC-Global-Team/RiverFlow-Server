package com.riverflow.service.admin;

import com.riverflow.dto.admin.AdminChangePasswordRequest;
import com.riverflow.dto.admin.AdminUpdateCreditRequest;
import com.riverflow.dto.admin.AdminUserRequest;
import com.riverflow.dto.admin.AdminUserResponse;
import com.riverflow.dto.payment.PaymentHistoryResponse;
import com.riverflow.model.User;
import com.riverflow.model.payment.PaymentTransaction;
import com.riverflow.repository.UserRepository;
import com.riverflow.repository.payment.PaymentTransactionRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for admin user management operations
 */
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Get all users with search, filter, sort, and pagination
     */
    @Transactional(readOnly = true)
    public Page<AdminUserResponse> getAllUsers(
            String search,
            String status,
            String role,
            String sortBy,
            String sortDir,
            int page,
            int size) {

        // Build sort
        Sort sort = Sort.by(sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        // Build specification for dynamic filtering
        Specification<User> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Search by email or fullName
            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.toLowerCase() + "%";
                Predicate emailPredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("email")), searchPattern);
                Predicate namePredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("fullName")), searchPattern);
                predicates.add(criteriaBuilder.or(emailPredicate, namePredicate));
            }

            // Filter by status (exclude deleted users by default if no status filter
            // provided)
            if (status != null && !status.trim().isEmpty()) {
                try {
                    User.UserStatus userStatus = User.UserStatus.valueOf(status.toLowerCase());
                    predicates.add(criteriaBuilder.equal(root.get("status"), userStatus));
                } catch (IllegalArgumentException ignored) {
                    // Invalid status, ignore filter
                }
            } else {
                // By default, exclude deleted users
                predicates.add(criteriaBuilder.notEqual(root.get("status"), User.UserStatus.deleted));
            }

            // Filter by role
            if (role != null && !role.trim().isEmpty()) {
                try {
                    User.Role userRole = User.Role.valueOf(role.toLowerCase());
                    predicates.add(criteriaBuilder.equal(root.get("role"), userRole));
                } catch (IllegalArgumentException ignored) {
                    // Invalid role, ignore filter
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<User> userPage = userRepository.findAll(spec, pageable);
        return userPage.map(this::mapToAdminUserResponse);
    }

    /**
     * Get user by ID
     */
    @Transactional(readOnly = true)
    public AdminUserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found with id: " + userId));
        return mapToAdminUserResponse(user);
    }

    /**
     * Update user information
     */
    @Transactional
    public AdminUserResponse updateUser(Long userId, AdminUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found with id: " + userId));

        // Update fields
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            // Check if email is already taken
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Email already in use");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getRole() != null) {
            try {
                user.setRole(User.Role.valueOf(request.getRole().toLowerCase()));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Invalid role: " + request.getRole());
            }
        }
        if (request.getStatus() != null) {
            try {
                user.setStatus(User.UserStatus.valueOf(request.getStatus().toLowerCase()));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Invalid status: " + request.getStatus());
            }
        }
        if (request.getPreferredLanguage() != null) {
            user.setPreferredLanguage(request.getPreferredLanguage());
        }
        if (request.getTimezone() != null) {
            user.setTimezone(request.getTimezone());
        }
        if (request.getTheme() != null) {
            try {
                user.setTheme(User.Theme.valueOf(request.getTheme().toLowerCase()));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Invalid theme: " + request.getTheme());
            }
        }

        userRepository.save(user);
        return mapToAdminUserResponse(user);
    }

    /**
     * Soft delete user (set status to 'deleted')
     */
    @Transactional
    public void softDeleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found with id: " + userId));

        user.setStatus(User.UserStatus.deleted);
        userRepository.save(user);
    }

    /**
     * Update user credit
     */
    @Transactional
    public AdminUserResponse updateUserCredit(Long userId, AdminUpdateCreditRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found with id: " + userId));

        user.setCredit(request.getCredit());
        userRepository.save(user);
        return mapToAdminUserResponse(user);
    }

    /**
     * Change user password
     */
    @Transactional
    public void changeUserPassword(Long userId, AdminChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found with id: " + userId));

        // Check if user is OAuth user
        if (user.getOauthProvider() != User.OAuthProvider.email) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Cannot change password for OAuth users");
        }

        // Hash and set new password
        String hashedPassword = passwordEncoder.encode(request.getNewPassword());
        user.setPasswordHash(hashedPassword);
        userRepository.save(user);
    }

    /**
     * Get user payment history
     */
    @Transactional(readOnly = true)
    public Page<PaymentHistoryResponse> getUserPaymentHistory(Long userId, int page, int size) {
        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "User not found with id: " + userId);
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<PaymentTransaction> transactionPage = paymentTransactionRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable);

        return transactionPage.map(this::mapToPaymentHistoryResponse);
    }

    /**
     * Map User entity to AdminUserResponse DTO
     */
    private AdminUserResponse mapToAdminUserResponse(User user) {
        // Generate avatar URL
        String avatarUrl = null;
        if (user.getAvatarData() != null && user.getAvatarData().length > 0) {
            avatarUrl = "/user/avatar/" + user.getId();
        } else if (user.getAvatar() != null) {
            avatarUrl = user.getAvatar();
        }

        return AdminUserResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatar(avatarUrl)
                .role(user.getRole() != null ? user.getRole().name() : "user")
                .credit(user.getCredit() != null ? user.getCredit() : 0L)
                .status(user.getStatus() != null ? user.getStatus().name() : "active")
                .preferredLanguage(user.getPreferredLanguage())
                .timezone(user.getTimezone())
                .theme(user.getTheme() != null ? user.getTheme().name() : "light")
                .emailVerified(user.getEmailVerified())
                .oauthProvider(user.getOauthProvider() != null ? user.getOauthProvider().name() : "email")
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    /**
     * Map PaymentTransaction entity to PaymentHistoryResponse DTO
     */
    private PaymentHistoryResponse mapToPaymentHistoryResponse(PaymentTransaction transaction) {
        return PaymentHistoryResponse.builder()
                .id(transaction.getId())
                .transactionCode(transaction.getCode())
                .amount(transaction.getTransferAmount())
                .status(transaction.getStatus() != null ? transaction.getStatus().name() : "unknown")
                .date(transaction.getCreatedAt())
                .gateway(transaction.getGateway())
                .content(transaction.getContent())
                .build();
    }
}
