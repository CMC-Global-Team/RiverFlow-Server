package com.riverflow.repository;

import com.riverflow.model.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for UserSubscription entity
 */
@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {
    
    /**
     * Count active subscriptions for a package
     */
    @Query("SELECT COUNT(us) FROM UserSubscription us WHERE us.packageEntity.id = :packageId AND us.status = 'active'")
    long countActiveSubscriptionsByPackageId(@Param("packageId") Long packageId);
    
    /**
     * Find all subscriptions for a package
     */
    List<UserSubscription> findByPackageEntityId(Long packageId);
    
    /**
     * Count total active subscriptions
     */
    @Query("SELECT COUNT(us) FROM UserSubscription us WHERE us.status = 'active'")
    long countActiveSubscriptions();
}

