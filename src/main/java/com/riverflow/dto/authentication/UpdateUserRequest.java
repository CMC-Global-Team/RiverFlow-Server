package com.riverflow.dto.authentication;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating user profile
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRequest {

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(min = 2, max = 100, message = "Họ và tên phải có từ 2 đến 100 ký tự")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @Size(max = 500, message = "URL avatar không được vượt quá 500 ký tự")
    private String avatar;

    @Size(max = 10, message = "Mã ngôn ngữ không được vượt quá 10 ký tự")
    private String preferredLanguage;

    @Size(max = 50, message = "Múi giờ không được vượt quá 50 ký tự")
    private String timezone;
}

