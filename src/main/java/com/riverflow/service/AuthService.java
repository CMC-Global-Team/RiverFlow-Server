package com.riverflow.service;

import com.riverflow.dto.auth.RegisterRequest;
import com.riverflow.model.User;

public interface AuthService {

    /**
     * Xử lý logic đăng ký người dùng mới.
     * @param registerRequest Thông tin đăng ký từ DTO
     * @return Đối tượng User đã được lưu
     */
    User registerUser(RegisterRequest registerRequest);

    /**
     * Xác thực email của người dùng dựa trên token.
     * @param token Token từ link xác thực
     */
    void verifyEmail(String token);
}