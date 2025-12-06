package com.riverflow.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riverflow.config.jwt.CustomUserDetailsService;
import com.riverflow.dto.admin.AdminChangePasswordRequest;
import com.riverflow.dto.admin.AdminUpdateCreditRequest;
import com.riverflow.dto.admin.AdminUserRequest;
import com.riverflow.dto.admin.AdminUserResponse;
import com.riverflow.dto.payment.PaymentHistoryResponse;
import com.riverflow.service.admin.AdminUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for AdminUserController using MockMvc
 */
@WebMvcTest(controllers = AdminUserController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class
})
@Import(com.riverflow.config.TestSecurityConfig.class)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminUserService adminUserService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    private AdminUserResponse testUserResponse;
    private AdminUserResponse adminUserResponse;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        testUserResponse = AdminUserResponse.builder()
                .userId(1L)
                .email("user@example.com")
                .fullName("Test User")
                .role("user")
                .status("active")
                .credit(100L)
                .preferredLanguage("en")
                .timezone("UTC")
                .theme("light")
                .emailVerified(true)
                .oauthProvider("email")
                .createdAt(now)
                .updatedAt(now)
                .build();

        adminUserResponse = AdminUserResponse.builder()
                .userId(2L)
                .email("admin@example.com")
                .fullName("Admin User")
                .role("admin")
                .status("active")
                .credit(1000L)
                .preferredLanguage("vi")
                .timezone("Asia/Ho_Chi_Minh")
                .theme("dark")
                .emailVerified(true)
                .oauthProvider("email")
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    @Nested
    @DisplayName("GET /admin/users Tests")
    class GetAllUsersTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return paginated users")
        void getAllUsers_AsAdmin_ReturnsUsers() throws Exception {
            // Given
            Page<AdminUserResponse> userPage = new PageImpl<>(
                    List.of(testUserResponse, adminUserResponse));
            when(adminUserService.getAllUsers(
                    isNull(), isNull(), isNull(),
                    eq("createdAt"), eq("desc"),
                    eq(0), eq(10)))
                    .thenReturn(userPage);

            // When & Then
            mockMvc.perform(get("/admin/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.content[0].email").value("user@example.com"))
                    .andExpect(jsonPath("$.content[1].email").value("admin@example.com"));

            verify(adminUserService).getAllUsers(
                    isNull(), isNull(), isNull(),
                    eq("createdAt"), eq("desc"),
                    eq(0), eq(10));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should filter users by search term")
        void getAllUsers_WithSearch_ReturnsFilteredUsers() throws Exception {
            // Given
            Page<AdminUserResponse> userPage = new PageImpl<>(List.of(testUserResponse));
            when(adminUserService.getAllUsers(
                    eq("user@example"), isNull(), isNull(),
                    eq("createdAt"), eq("desc"),
                    eq(0), eq(10)))
                    .thenReturn(userPage);

            // When & Then
            mockMvc.perform(get("/admin/users")
                    .param("search", "user@example"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].email").value("user@example.com"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should filter users by status")
        void getAllUsers_WithStatusFilter_ReturnsFilteredUsers() throws Exception {
            // Given
            Page<AdminUserResponse> userPage = new PageImpl<>(List.of(testUserResponse));
            when(adminUserService.getAllUsers(
                    isNull(), eq("active"), isNull(),
                    eq("createdAt"), eq("desc"),
                    eq(0), eq(10)))
                    .thenReturn(userPage);

            // When & Then
            mockMvc.perform(get("/admin/users")
                    .param("status", "active"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].status").value("active"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should filter users by role")
        void getAllUsers_WithRoleFilter_ReturnsFilteredUsers() throws Exception {
            // Given
            Page<AdminUserResponse> userPage = new PageImpl<>(List.of(adminUserResponse));
            when(adminUserService.getAllUsers(
                    isNull(), isNull(), eq("admin"),
                    eq("createdAt"), eq("desc"),
                    eq(0), eq(10)))
                    .thenReturn(userPage);

            // When & Then
            mockMvc.perform(get("/admin/users")
                    .param("role", "admin"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].role").value("admin"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should support pagination")
        void getAllUsers_WithPagination_ReturnsPaginatedUsers() throws Exception {
            // Given
            Page<AdminUserResponse> userPage = new PageImpl<>(List.of(testUserResponse));
            when(adminUserService.getAllUsers(
                    isNull(), isNull(), isNull(),
                    eq("createdAt"), eq("desc"),
                    eq(1), eq(5)))
                    .thenReturn(userPage);

            // When & Then
            mockMvc.perform(get("/admin/users")
                    .param("page", "1")
                    .param("size", "5"))
                    .andExpect(status().isOk());

            verify(adminUserService).getAllUsers(
                    isNull(), isNull(), isNull(),
                    eq("createdAt"), eq("desc"),
                    eq(1), eq(5));
        }
    }

    @Nested
    @DisplayName("GET /admin/users/{id} Tests")
    class GetUserByIdTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return user by ID")
        void getUserById_ValidId_ReturnsUser() throws Exception {
            // Given
            when(adminUserService.getUserById(1L)).thenReturn(testUserResponse);

            // When & Then
            mockMvc.perform(get("/admin/users/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(1L))
                    .andExpect(jsonPath("$.email").value("user@example.com"))
                    .andExpect(jsonPath("$.fullName").value("Test User"))
                    .andExpect(jsonPath("$.role").value("user"))
                    .andExpect(jsonPath("$.credit").value(100));

            verify(adminUserService).getUserById(1L);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return 404 for non-existent user")
        void getUserById_UserNotFound_Returns404() throws Exception {
            // Given
            when(adminUserService.getUserById(999L))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

            // When & Then
            mockMvc.perform(get("/admin/users/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /admin/users/{id} Tests")
    class UpdateUserTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should update user successfully")
        void updateUser_ValidRequest_ReturnsUpdatedUser() throws Exception {
            // Given
            AdminUserRequest request = new AdminUserRequest();
            request.setFullName("Updated Name");
            request.setEmail("updated@example.com");
            request.setRole("admin");
            request.setStatus("active");

            AdminUserResponse updatedResponse = AdminUserResponse.builder()
                    .userId(1L)
                    .email("updated@example.com")
                    .fullName("Updated Name")
                    .role("admin")
                    .status("active")
                    .credit(100L)
                    .build();

            when(adminUserService.updateUser(eq(1L), any(AdminUserRequest.class)))
                    .thenReturn(updatedResponse);

            // When & Then
            mockMvc.perform(put("/admin/users/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("updated@example.com"))
                    .andExpect(jsonPath("$.fullName").value("Updated Name"))
                    .andExpect(jsonPath("$.role").value("admin"));

            verify(adminUserService).updateUser(eq(1L), any(AdminUserRequest.class));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return 404 for non-existent user")
        void updateUser_UserNotFound_Returns404() throws Exception {
            // Given
            AdminUserRequest request = new AdminUserRequest();
            request.setFullName("Updated Name");

            when(adminUserService.updateUser(eq(999L), any(AdminUserRequest.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

            // When & Then
            mockMvc.perform(put("/admin/users/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /admin/users/{id} Tests")
    class DeleteUserTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should soft delete user successfully")
        void deleteUser_ValidId_Returns204() throws Exception {
            // Given
            doNothing().when(adminUserService).softDeleteUser(1L);

            // When & Then
            mockMvc.perform(delete("/admin/users/1"))
                    .andExpect(status().isNoContent());

            verify(adminUserService).softDeleteUser(1L);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return 404 for non-existent user")
        void deleteUser_UserNotFound_Returns404() throws Exception {
            // Given
            doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
                    .when(adminUserService).softDeleteUser(999L);

            // When & Then
            mockMvc.perform(delete("/admin/users/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /admin/users/{id}/credit Tests")
    class UpdateUserCreditTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should update user credit successfully")
        void updateUserCredit_ValidRequest_ReturnsUpdatedUser() throws Exception {
            // Given
            AdminUpdateCreditRequest request = new AdminUpdateCreditRequest();
            request.setCredit(500L);

            AdminUserResponse updatedResponse = AdminUserResponse.builder()
                    .userId(1L)
                    .email("user@example.com")
                    .fullName("Test User")
                    .role("user")
                    .credit(500L)
                    .build();

            when(adminUserService.updateUserCredit(eq(1L), any(AdminUpdateCreditRequest.class)))
                    .thenReturn(updatedResponse);

            // When & Then
            mockMvc.perform(put("/admin/users/1/credit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.credit").value(500));

            verify(adminUserService).updateUserCredit(eq(1L), any(AdminUpdateCreditRequest.class));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return 404 for non-existent user")
        void updateUserCredit_UserNotFound_Returns404() throws Exception {
            // Given
            AdminUpdateCreditRequest request = new AdminUpdateCreditRequest();
            request.setCredit(500L);

            when(adminUserService.updateUserCredit(eq(999L), any(AdminUpdateCreditRequest.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

            // When & Then
            mockMvc.perform(put("/admin/users/999/credit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /admin/users/{id}/password Tests")
    class ChangeUserPasswordTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should change password successfully")
        void changeUserPassword_ValidRequest_Returns204() throws Exception {
            // Given
            AdminChangePasswordRequest request = new AdminChangePasswordRequest();
            request.setNewPassword("newPassword123");

            doNothing().when(adminUserService).changeUserPassword(eq(1L), any(AdminChangePasswordRequest.class));

            // When & Then
            mockMvc.perform(put("/admin/users/1/password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            verify(adminUserService).changeUserPassword(eq(1L), any(AdminChangePasswordRequest.class));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return 400 for OAuth user")
        void changeUserPassword_OAuthUser_Returns400() throws Exception {
            // Given
            AdminChangePasswordRequest request = new AdminChangePasswordRequest();
            request.setNewPassword("newPassword123");

            doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot change password for OAuth users"))
                    .when(adminUserService).changeUserPassword(eq(1L), any(AdminChangePasswordRequest.class));

            // When & Then
            mockMvc.perform(put("/admin/users/1/password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /admin/users/{id}/payments Tests")
    class GetUserPaymentHistoryTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return payment history")
        void getUserPaymentHistory_ValidUser_ReturnsHistory() throws Exception {
            // Given
            PaymentHistoryResponse payment = PaymentHistoryResponse.builder()
                    .id(1L)
                    .transactionCode("TXN001")
                    .amount(50000L)
                    .status("processed")
                    .gateway("VietQR")
                    .content("Credit purchase")
                    .date(now)
                    .build();

            Page<PaymentHistoryResponse> paymentPage = new PageImpl<>(List.of(payment));
            when(adminUserService.getUserPaymentHistory(eq(1L), eq(0), eq(10)))
                    .thenReturn(paymentPage);

            // When & Then
            mockMvc.perform(get("/admin/users/1/payments"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].transactionCode").value("TXN001"))
                    .andExpect(jsonPath("$.content[0].amount").value(50000));

            verify(adminUserService).getUserPaymentHistory(eq(1L), eq(0), eq(10));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return 404 for non-existent user")
        void getUserPaymentHistory_UserNotFound_Returns404() throws Exception {
            // Given
            when(adminUserService.getUserPaymentHistory(eq(999L), eq(0), eq(10)))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

            // When & Then
            mockMvc.perform(get("/admin/users/999/payments"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should support pagination for payment history")
        void getUserPaymentHistory_WithPagination_ReturnsPaginatedHistory() throws Exception {
            // Given
            Page<PaymentHistoryResponse> paymentPage = Page.empty();
            when(adminUserService.getUserPaymentHistory(eq(1L), eq(2), eq(20)))
                    .thenReturn(paymentPage);

            // When & Then
            mockMvc.perform(get("/admin/users/1/payments")
                    .param("page", "2")
                    .param("size", "20"))
                    .andExpect(status().isOk());

            verify(adminUserService).getUserPaymentHistory(eq(1L), eq(2), eq(20));
        }
    }
}
