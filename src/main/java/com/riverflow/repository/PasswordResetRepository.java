package com.riverflow.repository;

import com.riverflow.model.PasswordReset;
import org.springframework.data.jpa.repository.JpaRepository;


public interface PasswordResetRepository extends JpaRepository<PasswordReset, Long> {
}