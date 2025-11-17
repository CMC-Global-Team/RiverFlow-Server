package com.riverflow.service.authentication;

import com.riverflow.dto.authentication.ChangePasswordRequest;
import com.riverflow.model.User;
import com.riverflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ChangePassService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void changePassword(String email, ChangePasswordRequest request) {

        // 1. Tìm user theo email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));

        // 2. Không cho đổi nếu là user OAuth
        if (user.getOauthProvider() != User.OAuthProvider.email) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Tài khoản OAuth không thể đổi mật khẩu");
        }

        // 3. Kiểm tra mật khẩu hiện tại
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Mật khẩu hiện tại không chính xác");
        }

        // 4. Kiểm tra mật khẩu mới trùng confirm
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Mật khẩu mới không khớp");
        }

        // 5. Cập nhật mật khẩu mới
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));

        // 6. Lưu lại
        userRepository.save(user);
    }
}
