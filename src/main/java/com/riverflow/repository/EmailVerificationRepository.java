package com.riverflow.repository;

import com.riverflow.model.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findByToken(String token);
    Optional<EmailVerification> findFirstByUserIdAndVerifiedAtIsNullOrderByCreatedAtDesc(Long userId);
}