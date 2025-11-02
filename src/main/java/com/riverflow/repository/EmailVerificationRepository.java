package com.riverflow.repository;

import com.riverflow.model.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    /**
     * Tự động tạo câu lệnh: "SELECT ... FROM email_verifications WHERE token = ?"
     * Dùng để tìm token khi người dùng nhấn vào link xác thực.
     */
    Optional<EmailVerification> findByToken(String token);
}