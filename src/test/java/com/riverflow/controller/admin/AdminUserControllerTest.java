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
import org.springframework.data.domain.PageRequest;
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
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import com.riverflow.config.jwt.UserPrincipal;
import com.riverflow.model.User;

/**
 * Controller tests for AdminUserController using MockMvc
 */
@WebMvcTest(controllers = AdminUserController.class, excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class,
                org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration.class,
                org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration.class
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

        // ============= GET /admin/users Tests =============

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("GET /admin/users - Should return paginated users")
        void getAllUsers_AsAdmin_ReturnsUsers() throws Exception {
                // Given
                List<AdminUserResponse> userList = List.of(testUserResponse, adminUserResponse);
                Page<AdminUserResponse> userPage = new PageImpl<>(userList, PageRequest.of(0, 10), userList.size());
                when(adminUserService.getAllUsers(any(), any(), any(), any(), any(), anyInt(), anyInt(), anyBoolean()))
                                .thenReturn(userPage);

                // When & Then
                mockMvc.perform(get("/admin/users"))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.content.length()").value(2))
                                .andExpect(jsonPath("$.content[0].email").value("user@example.com"))
                                .andExpect(jsonPath("$.content[1].email").value("admin@example.com"));

                verify(adminUserService).getAllUsers(any(), any(), any(), any(), any(), anyInt(), anyInt(),
                                anyBoolean());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("GET /admin/users - Should filter users by search term")
        void getAllUsers_WithSearch_ReturnsFilteredUsers() throws Exception {
                // Given
                List<AdminUserResponse> content = List.of(testUserResponse);
                Page<AdminUserResponse> userPage = new PageImpl<>(content, PageRequest.of(0, 10), content.size());
                when(adminUserService.getAllUsers(any(), any(), any(), any(), any(), anyInt(), anyInt(), anyBoolean()))
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
        @DisplayName("GET /admin/users - Should filter users by status")
        void getAllUsers_WithStatusFilter_ReturnsFilteredUsers() throws Exception {
                // Given
                List<AdminUserResponse> content = List.of(testUserResponse);
                Page<AdminUserResponse> userPage = new PageImpl<>(content, PageRequest.of(0, 10), content.size());
                when(adminUserService.getAllUsers(any(), any(), any(), any(), any(), anyInt(), anyInt(), anyBoolean()))
                                .thenReturn(userPage);

                // When & Then
                mockMvc.perform(get("/admin/users")
                                .param("status", "active"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content[0].status").value("active"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("GET /admin/users - Should filter users by role")
        void getAllUsers_WithRoleFilter_ReturnsFilteredUsers() throws Exception {
                // Given
                List<AdminUserResponse> content = List.of(adminUserResponse);
                Page<AdminUserResponse> userPage = new PageImpl<>(content, PageRequest.of(0, 10), content.size());
                when(adminUserService.getAllUsers(any(), any(), any(), any(), any(), anyInt(), anyInt(), anyBoolean()))
                                .thenReturn(userPage);

                // When & Then
                mockMvc.perform(get("/admin/users")
                                .param("role", "admin"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content[0].role").value("admin"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("GET /admin/users - Should support pagination")
        void getAllUsers_WithPagination_ReturnsPaginatedUsers() throws Exception {
                // Given
                List<AdminUserResponse> content = List.of(testUserResponse);
                Page<AdminUserResponse> userPage = new PageImpl<>(content, PageRequest.of(1, 5), content.size());
                when(adminUserService.getAllUsers(any(), any(), any(), any(), any(), eq(1), eq(5), anyBoolean()))
                                .thenReturn(userPage);

                // When & Then
                mockMvc.perform(get("/admin/users")
                                .param("page", "1")
                                .param("size", "5"))
                                .andExpect(status().isOk());

                verify(adminUserService).getAllUsers(any(), any(), any(), any(), any(), eq(1), eq(5), anyBoolean());
        }

        // ============= GET /admin/users/{id} Tests =============

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("GET /admin/users/{id} - Should return user by ID")
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
        @DisplayName("GET /admin/users/{id} - Should return 404 for non-existent user")
        void getUserById_UserNotFound_Returns404() throws Exception {
                // Given
                when(adminUserService.getUserById(999L))
                                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

                // When & Then
                mockMvc.perform(get("/admin/users/999"))
                                .andExpect(status().isNotFound());
        }

        // ============= PUT /admin/users/{id} Tests =============

        @Test
        @DisplayName("PUT /admin/users/{id} - Should update user successfully")
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

                // Create a proper UserPrincipal for injection
                UserPrincipal mockAdmin = new UserPrincipal(
                                1L, "admin@test.com", "password", true, false,
                                User.Role.super_admin,
                                java.util.Collections.singleton(
                                                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                                                "ROLE_ADMIN")));

                when(adminUserService.updateUser(eq(1L), any(AdminUserRequest.class), any()))
                                .thenReturn(updatedResponse);

                // When & Then
                mockMvc.perform(put("/admin/users/1")
                                .with(user(mockAdmin))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.email").value("updated@example.com"))
                                .andExpect(jsonPath("$.fullName").value("Updated Name"))
                                .andExpect(jsonPath("$.role").value("admin"));

                verify(adminUserService).updateUser(eq(1L), any(AdminUserRequest.class), any());
        }

        @Test
        @DisplayName("PUT /admin/users/{id} - Should return 404 for non-existent user")
        void updateUser_UserNotFound_Returns404() throws Exception {
                // Given
                AdminUserRequest request = new AdminUserRequest();
                request.setFullName("Updated Name");

                // Create a proper UserPrincipal for injection
                UserPrincipal mockAdmin = new UserPrincipal(
                                1L, "admin@test.com", "password", true, false,
                                User.Role.super_admin,
                                java.util.Collections.singleton(
                                                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                                                "ROLE_ADMIN")));

                when(adminUserService.updateUser(eq(999L), any(AdminUserRequest.class), any()))
                                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

                // When & Then
                mockMvc.perform(put("/admin/users/999")
                                .with(user(mockAdmin))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNotFound());
        }

        // ============= DELETE /admin/users/{id} Tests =============

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("DELETE /admin/users/{id} - Should soft delete user successfully")
        void deleteUser_ValidId_Returns200() throws Exception {
                // Given
                doNothing().when(adminUserService).softDeleteUser(1L);

                // When & Then
                mockMvc.perform(delete("/admin/users/1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("User deleted successfully"));

                verify(adminUserService).softDeleteUser(1L);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("DELETE /admin/users/{id} - Should return 404 for non-existent user")
        void deleteUser_UserNotFound_Returns404() throws Exception {
                // Given
                doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
                                .when(adminUserService).softDeleteUser(999L);

                // When & Then
                mockMvc.perform(delete("/admin/users/999"))
                                .andExpect(status().isNotFound());
        }

        // ============= PUT /admin/users/{id}/credit Tests =============

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("PUT /admin/users/{id}/credit - Should update user credit successfully")
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
        @DisplayName("PUT /admin/users/{id}/credit - Should return 404 for non-existent user")
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

        // ============= PUT /admin/users/{id}/password Tests =============

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("PUT /admin/users/{id}/password - Should change password successfully")
        void changeUserPassword_ValidRequest_Returns200() throws Exception {
                // Given
                AdminChangePasswordRequest request = new AdminChangePasswordRequest();
                request.setNewPassword("newPassword123");

                doNothing().when(adminUserService).changeUserPassword(eq(1L), any(AdminChangePasswordRequest.class));

                // When & Then
                mockMvc.perform(put("/admin/users/1/password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Password changed successfully"));

                verify(adminUserService).changeUserPassword(eq(1L), any(AdminChangePasswordRequest.class));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("PUT /admin/users/{id}/password - Should return 400 for OAuth user")
        void changeUserPassword_OAuthUser_Returns400() throws Exception {
                // Given
                AdminChangePasswordRequest request = new AdminChangePasswordRequest();
                request.setNewPassword("newPassword123");

                doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot change password for OAuth users"))
                                .when(adminUserService)
                                .changeUserPassword(eq(1L), any(AdminChangePasswordRequest.class));

                // When & Then
                mockMvc.perform(put("/admin/users/1/password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        // ============= GET /admin/users/{id}/payments Tests =============

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("GET /admin/users/{id}/payments - Should return payment history")
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

                List<PaymentHistoryResponse> paymentList = List.of(payment);
                Page<PaymentHistoryResponse> paymentPage = new PageImpl<>(paymentList, PageRequest.of(0, 10),
                                paymentList.size());
                when(adminUserService.getUserPaymentHistory(anyLong(), anyInt(), anyInt()))
                                .thenReturn(paymentPage);

                // When & Then
                mockMvc.perform(get("/admin/users/1/payments"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.content[0].transactionCode").value("TXN001"))
                                .andExpect(jsonPath("$.content[0].amount").value(50000));

                verify(adminUserService).getUserPaymentHistory(eq(1L), anyInt(), anyInt());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("GET /admin/users/{id}/payments - Should return 404 for non-existent user")
        void getUserPaymentHistory_UserNotFound_Returns404() throws Exception {
                // Given
                when(adminUserService.getUserPaymentHistory(eq(999L), anyInt(), anyInt()))
                                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

                // When & Then
                mockMvc.perform(get("/admin/users/999/payments"))
                                .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("GET /admin/users/{id}/payments - Should support pagination for payment history")
        void getUserPaymentHistory_WithPagination_ReturnsPaginatedHistory() throws Exception {
                // Given
                Page<PaymentHistoryResponse> paymentPage = new PageImpl<>(List.of(), PageRequest.of(2, 20), 0);
                when(adminUserService.getUserPaymentHistory(anyLong(), anyInt(), anyInt()))
                                .thenReturn(paymentPage);

                // When & Then
                mockMvc.perform(get("/admin/users/1/payments")
                                .param("page", "2")
                                .param("size", "20"))
                                .andExpect(status().isOk());

                verify(adminUserService).getUserPaymentHistory(eq(1L), eq(2), eq(20));
        }
}
