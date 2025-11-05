package com.riverflow.repository;

import com.riverflow.model.UserActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {
    
    List<UserActivity> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<UserActivity> findByActivityType(String activityType);
    
    List<UserActivity> findByEntityTypeAndEntityId(String entityType, String entityId);
    
    List<UserActivity> findByUserIdAndCreatedAtAfter(Long userId, LocalDateTime after);
}

