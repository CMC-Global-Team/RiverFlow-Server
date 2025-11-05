package com.riverflow.repository;

import com.riverflow.model.SaasIntegration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SaasIntegrationRepository extends JpaRepository<SaasIntegration, Long> {
    
    List<SaasIntegration> findByUserId(Long userId);
    
    Optional<SaasIntegration> findByUserIdAndPlatformName(Long userId, String platformName);
    
    List<SaasIntegration> findByStatus(SaasIntegration.IntegrationStatus status);
}

