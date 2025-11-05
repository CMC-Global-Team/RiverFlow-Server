package com.riverflow.repository;

import com.riverflow.model.AiWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiWorkflowRepository extends JpaRepository<AiWorkflow, Long> {
    
    Optional<AiWorkflow> findBySlug(String slug);
    
    List<AiWorkflow> findByIsActiveOrderByDisplayOrderAsc(Boolean isActive);
    
    List<AiWorkflow> findByCategoryIdAndIsActive(Long categoryId, Boolean isActive);
    
    List<AiWorkflow> findByIsFeaturedAndIsActive(Boolean isFeatured, Boolean isActive);
    
    List<AiWorkflow> findTop10ByIsActiveOrderByUsageCountDesc(Boolean isActive);
}

