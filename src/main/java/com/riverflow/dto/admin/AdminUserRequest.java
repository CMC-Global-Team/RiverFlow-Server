package com.riverflow.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for admin to update user information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @Email(message = "Invalid email format")
    private String email;

    private String role; // "admin" or "user"

    private String status; // "active", "suspended", "deleted"

    @Size(max = 10, message = "Language code too long")
    private String preferredLanguage;

    @Size(max = 50, message = "Timezone too long")
    private String timezone;

    private String theme; // "light", "dark", "auto"
}
