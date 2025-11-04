package com.riverflow.repository;

import com.riverflow.model.PasswordReset;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

public interface PasswordResetRepository extends JpaRepository<PasswordReset, Long> {
    Optional<PasswordReset> findByToken(String token);
    List<PasswordReset> findByUserIdAndUsedAtIsNull(Long userId);
    void deleteByExpiresAtBefore(LocalDateTime now);
}
