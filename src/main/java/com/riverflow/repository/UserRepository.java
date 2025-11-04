package com.riverflow.repository;

import com.riverflow.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Tự động tạo câu lệnh: "SELECT ... FROM users WHERE email = ?"
     * Dùng để tìm user khi đăng nhập.
     */
    Optional<User> findByEmail(String email);

    /**
     * Tự động tạo câu lệnh: "SELECT COUNT(*) > 0 FROM users WHERE email = ?"
     * Dùng để kiểm tra email đã tồn tại hay chưa (hiệu quả hơn là lấy cả object).
     */
    Boolean existsByEmail(String email);
}