package com.riverflow.repository;

import com.riverflow.model.PasswordReset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetRepository extends JpaRepository<PasswordReset, Long> {

    /**
     * Finds a valid (unused + not expired) reset token.
     */
    Optional<PasswordReset> findByTokenAndUsedAtIsNullAndExpiresAtAfter(
            String token, LocalDateTime now);
}