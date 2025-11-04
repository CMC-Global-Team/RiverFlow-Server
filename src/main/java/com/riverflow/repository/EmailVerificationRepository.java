package com.riverflow.repository;

import com.riverflow.model.EmailVerification;
import com.riverflow.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findByToken(String token);
    void deleteAllByUser(User user);
}
