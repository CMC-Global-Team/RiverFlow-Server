package com.riverflow.repository;

import com.riverflow.model.AiWorkflowCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiWorkflowCategoryRepository extends JpaRepository<AiWorkflowCategory, Long> {
    
    Optional<AiWorkflowCategory> findBySlug(String slug);
    
    List<AiWorkflowCategory> findByIsActiveOrderByDisplayOrder(Boolean isActive);
}

