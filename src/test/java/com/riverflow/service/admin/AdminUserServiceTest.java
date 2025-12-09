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
import com.riverflow.service.logging.ActivityLoggingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AdminUserService
 */
@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ActivityLoggingService activityLoggingService;

    @InjectMocks
    private AdminUserService adminUserService;

    private User testUser;
    private User adminUser;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        testUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .fullName("Test User")
                .passwordHash("hashedPassword")
                .oauthProvider(User.OAuthProvider.email)
                .emailVerified(true)
                .role(User.Role.user)
                .credit(100L)
                .preferredLanguage("en")
                .timezone("UTC")
                .theme(User.Theme.light)
                .status(User.UserStatus.active)
                .build();
        testUser.setCreatedAt(now);
        testUser.setUpdatedAt(now);
        testUser.setLastLoginAt(now);

        adminUser = User.builder()
                .id(2L)
                .email("admin@example.com")
                .fullName("Admin User")
                .passwordHash("adminHash")
                .oauthProvider(User.OAuthProvider.email)
                .emailVerified(true)
                .role(User.Role.admin)
                .credit(1000L)
                .preferredLanguage("vi")
                .timezone("Asia/Ho_Chi_Minh")
                .theme(User.Theme.dark)
                .status(User.UserStatus.active)
                .build();
        adminUser.setCreatedAt(now);
        adminUser.setUpdatedAt(now);
    }

    @Nested
    @DisplayName("getAllUsers Tests")
    class GetAllUsersTests {

        @Test
        @DisplayName("Should return paginated users with default filters")
        void getAllUsers_DefaultFilters_ReturnsPaginatedUsers() {
            // Given
            Page<User> userPage = new PageImpl<>(List.of(testUser, adminUser));
            when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(userPage);

            // When
            Page<AdminUserResponse> result = adminUserService.getAllUsers(
                    null, null, null, "createdAt", "desc", 0, 10, false);

            // Then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).getEmail()).isEqualTo("user@example.com");
            assertThat(result.getContent().get(1).getEmail()).isEqualTo("admin@example.com");
            verify(userRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Should filter users by search term")
        void getAllUsers_WithSearch_ReturnsFilteredUsers() {
            // Given
            Page<User> userPage = new PageImpl<>(List.of(testUser));
            when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(userPage);

            // When
            Page<AdminUserResponse> result = adminUserService.getAllUsers(
                    "user@example", null, null, "createdAt", "desc", 0, 10, false);

            // Then
            assertThat(result.getContent()).hasSize(1);
            verify(userRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Should filter users by status")
        void getAllUsers_WithStatusFilter_ReturnsFilteredUsers() {
            // Given
            Page<User> userPage = new PageImpl<>(List.of(testUser));
            when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(userPage);

            // When
            Page<AdminUserResponse> result = adminUserService.getAllUsers(
                    null, "active", null, "createdAt", "desc", 0, 10, false);

            // Then
            assertThat(result.getContent()).hasSize(1);
            verify(userRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Should filter users by role")
        void getAllUsers_WithRoleFilter_ReturnsFilteredUsers() {
            // Given
            Page<User> userPage = new PageImpl<>(List.of(adminUser));
            when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(userPage);

            // When
            Page<AdminUserResponse> result = adminUserService.getAllUsers(
                    null, null, "admin", "createdAt", "desc", 0, 10, false);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getRole()).isEqualTo("admin");
            verify(userRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Should sort users in ascending order")
        void getAllUsers_WithAscSort_ReturnsSortedUsers() {
            // Given
            Page<User> userPage = new PageImpl<>(List.of(testUser, adminUser));
            when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(userPage);

            // When
            Page<AdminUserResponse> result = adminUserService.getAllUsers(
                    null, null, null, "email", "asc", 0, 10, false);

            // Then
            assertThat(result.getContent()).hasSize(2);
            verify(userRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Should handle empty results")
        void getAllUsers_NoUsers_ReturnsEmptyPage() {
            // Given
            Page<User> emptyPage = Page.empty();
            when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(emptyPage);

            // When
            Page<AdminUserResponse> result = adminUserService.getAllUsers(
                    "nonexistent", null, null, "createdAt", "desc", 0, 10, false);

            // Then
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("Should ignore invalid status filter")
        void getAllUsers_InvalidStatus_IgnoresFilter() {
            // Given
            Page<User> userPage = new PageImpl<>(List.of(testUser));
            when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(userPage);

            // When
            Page<AdminUserResponse> result = adminUserService.getAllUsers(
                    null, "invalid_status", null, "createdAt", "desc", 0, 10, false);

            // Then
            assertThat(result).isNotNull();
            verify(userRepository).findAll(any(Specification.class), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("getUserById Tests")
    class GetUserByIdTests {

        @Test
        @DisplayName("Should return user when found")
        void getUserById_ValidId_ReturnsUser() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When
            AdminUserResponse result = adminUserService.getUserById(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(1L);
            assertThat(result.getEmail()).isEqualTo("user@example.com");
            assertThat(result.getFullName()).isEqualTo("Test User");
            assertThat(result.getRole()).isEqualTo("user");
            assertThat(result.getCredit()).isEqualTo(100L);
            assertThat(result.getStatus()).isEqualTo("active");
            verify(userRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void getUserById_UserNotFound_ThrowsException() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> adminUserService.getUserById(999L))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("User not found");
            verify(userRepository).findById(999L);
        }

        @Test
        @DisplayName("Should return avatar URL when user has avatar data")
        void getUserById_WithAvatarData_ReturnsAvatarUrl() {
            // Given
            testUser.setAvatarData(new byte[] { 1, 2, 3 });
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When
            AdminUserResponse result = adminUserService.getUserById(1L);

            // Then
            assertThat(result.getAvatar()).isEqualTo("/user/avatar/1");
        }
    }

    @Nested
    @DisplayName("updateUser Tests")
    class UpdateUserTests {

        @Test
        @DisplayName("Should update user fields successfully")
        void updateUser_ValidRequest_UpdatesUser() {
            // Given
            AdminUserRequest request = new AdminUserRequest();
            request.setFullName("Updated Name");
            request.setEmail("updated@example.com");
            request.setRole("admin");
            request.setStatus("suspended");

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.existsByEmail("updated@example.com")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // When
            AdminUserResponse result = adminUserService.updateUser(1L, request, User.Role.super_admin, 2L,
                    "admin@test.com");

            // Then
            assertThat(result).isNotNull();
            verify(userRepository).findById(1L);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void updateUser_UserNotFound_ThrowsException() {
            // Given
            AdminUserRequest request = new AdminUserRequest();
            request.setFullName("Updated Name");

            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(
                    () -> adminUserService.updateUser(999L, request, User.Role.super_admin, 2L, "admin@test.com"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void updateUser_EmailAlreadyExists_ThrowsException() {
            // Given
            AdminUserRequest request = new AdminUserRequest();
            request.setEmail("existing@example.com");

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

            // When & Then
            assertThatThrownBy(
                    () -> adminUserService.updateUser(1L, request, User.Role.super_admin, 2L, "admin@test.com"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Email already in use");
        }

        @Test
        @DisplayName("Should throw exception for invalid role")
        void updateUser_InvalidRole_ThrowsException() {
            // Given
            AdminUserRequest request = new AdminUserRequest();
            request.setRole("invalid_role");

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When & Then
            assertThatThrownBy(
                    () -> adminUserService.updateUser(1L, request, User.Role.super_admin, 2L, "admin@test.com"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Invalid role");
        }

        @Test
        @DisplayName("Should throw exception for invalid status")
        void updateUser_InvalidStatus_ThrowsException() {
            // Given
            AdminUserRequest request = new AdminUserRequest();
            request.setStatus("invalid_status");

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When & Then
            assertThatThrownBy(
                    () -> adminUserService.updateUser(1L, request, User.Role.super_admin, 2L, "admin@test.com"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Invalid status");
        }
    }

    @Nested
    @DisplayName("softDeleteUser Tests")
    class SoftDeleteUserTests {

        @Test
        @DisplayName("Should soft delete user successfully")
        void softDeleteUser_ValidId_SetsStatusToDeleted() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // When
            adminUserService.softDeleteUser(1L, 2L, "admin@test.com", "ADMIN");

            // Then
            verify(userRepository).findById(1L);
            verify(userRepository).save(argThat(user -> user.getStatus() == User.UserStatus.deleted));
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void softDeleteUser_UserNotFound_ThrowsException() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> adminUserService.softDeleteUser(999L, 2L, "admin@test.com", "ADMIN"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("User not found");
        }
    }

    @Nested
    @DisplayName("updateUserCredit Tests")
    class UpdateUserCreditTests {

        @Test
        @DisplayName("Should update user credit successfully")
        void updateUserCredit_ValidRequest_UpdatesCredit() {
            // Given
            AdminUpdateCreditRequest request = new AdminUpdateCreditRequest();
            request.setCredit(500L);

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // When
            AdminUserResponse result = adminUserService.updateUserCredit(1L, request, 2L, "admin@test.com", "ADMIN");

            // Then
            assertThat(result).isNotNull();
            verify(userRepository).save(argThat(user -> user.getCredit() == 500L));
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void updateUserCredit_UserNotFound_ThrowsException() {
            // Given
            AdminUpdateCreditRequest request = new AdminUpdateCreditRequest();
            request.setCredit(500L);

            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> adminUserService.updateUserCredit(999L, request, 2L, "admin@test.com", "ADMIN"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("User not found");
        }
    }

    @Nested
    @DisplayName("changeUserPassword Tests")
    class ChangeUserPasswordTests {

        @Test
        @DisplayName("Should change password for email user")
        void changeUserPassword_EmailUser_ChangesPassword() {
            // Given
            AdminChangePasswordRequest request = new AdminChangePasswordRequest();
            request.setNewPassword("newPassword123");

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // When
            adminUserService.changeUserPassword(1L, request, 2L, "admin@test.com", "ADMIN");

            // Then
            verify(passwordEncoder).encode("newPassword123");
            verify(userRepository).save(argThat(user -> user.getPasswordHash().equals("encodedNewPassword")));
        }

        @Test
        @DisplayName("Should throw exception for OAuth user")
        void changeUserPassword_OAuthUser_ThrowsException() {
            // Given
            testUser.setOauthProvider(User.OAuthProvider.google);
            AdminChangePasswordRequest request = new AdminChangePasswordRequest();
            request.setNewPassword("newPassword123");

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When & Then
            assertThatThrownBy(() -> adminUserService.changeUserPassword(1L, request, 2L, "admin@test.com", "ADMIN"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Cannot change password for OAuth users");
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void changeUserPassword_UserNotFound_ThrowsException() {
            // Given
            AdminChangePasswordRequest request = new AdminChangePasswordRequest();
            request.setNewPassword("newPassword123");

            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> adminUserService.changeUserPassword(999L, request, 2L, "admin@test.com", "ADMIN"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("User not found");
        }
    }

    @Nested
    @DisplayName("getUserPaymentHistory Tests")
    class GetUserPaymentHistoryTests {

        @Test
        @DisplayName("Should return payment history for user")
        void getUserPaymentHistory_ValidUser_ReturnsHistory() {
            // Given
            PaymentTransaction transaction = PaymentTransaction.builder()
                    .id(1L)
                    .code("TXN001")
                    .transferAmount(50000L)
                    .status(PaymentTransaction.TransactionStatus.processed)
                    .gateway("VietQR")
                    .content("Credit purchase")
                    .build();
            transaction.setCreatedAt(now);

            Page<PaymentTransaction> transactionPage = new PageImpl<>(List.of(transaction));

            when(userRepository.existsById(1L)).thenReturn(true);
            when(paymentTransactionRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                    .thenReturn(transactionPage);

            // When
            Page<PaymentHistoryResponse> result = adminUserService.getUserPaymentHistory(1L, 0, 10);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTransactionCode()).isEqualTo("TXN001");
            assertThat(result.getContent().get(0).getAmount()).isEqualTo(50000L);
            assertThat(result.getContent().get(0).getStatus()).isEqualTo("processed");
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void getUserPaymentHistory_UserNotFound_ThrowsException() {
            // Given
            when(userRepository.existsById(999L)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> adminUserService.getUserPaymentHistory(999L, 0, 10))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("Should return empty page when no transactions")
        void getUserPaymentHistory_NoTransactions_ReturnsEmptyPage() {
            // Given
            when(userRepository.existsById(1L)).thenReturn(true);
            when(paymentTransactionRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                    .thenReturn(Page.empty());

            // When
            Page<PaymentHistoryResponse> result = adminUserService.getUserPaymentHistory(1L, 0, 10);

            // Then
            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Response Mapping Tests")
    class ResponseMappingTests {

        @Test
        @DisplayName("Should handle null values gracefully")
        void mapToAdminUserResponse_NullFields_ReturnsDefaults() {
            // Given
            User userWithNulls = User.builder()
                    .id(3L)
                    .email("nulluser@example.com")
                    .fullName("Null User")
                    .role(null)
                    .status(null)
                    .theme(null)
                    .credit(null)
                    .oauthProvider(null)
                    .build();

            when(userRepository.findById(3L)).thenReturn(Optional.of(userWithNulls));

            // When
            AdminUserResponse result = adminUserService.getUserById(3L);

            // Then
            assertThat(result.getRole()).isEqualTo("user");
            assertThat(result.getStatus()).isEqualTo("active");
            assertThat(result.getTheme()).isEqualTo("light");
            assertThat(result.getCredit()).isEqualTo(0L);
            assertThat(result.getOauthProvider()).isEqualTo("email");
        }

        @Test
        @DisplayName("Should return legacy avatar URL when no avatar data")
        void mapToAdminUserResponse_LegacyAvatar_ReturnsUrl() {
            // Given
            testUser.setAvatar("https://example.com/avatar.png");
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When
            AdminUserResponse result = adminUserService.getUserById(1L);

            // Then
            assertThat(result.getAvatar()).isEqualTo("https://example.com/avatar.png");
        }
    }
}
