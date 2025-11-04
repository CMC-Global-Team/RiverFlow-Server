package com.riverflow.repository;

import com.riverflow.model.RefreshToken;
import com.riverflow.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Dùng để tìm token khi người dùng muốn "refresh" (làm mới)
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Dùng để xóa tất cả refresh token của user (ví dụ: khi đổi mật khẩu)
     */
    void deleteAllByUser(User user);
}