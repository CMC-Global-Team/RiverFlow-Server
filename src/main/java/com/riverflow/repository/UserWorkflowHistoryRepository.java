package com.riverflow.repository;

import com.riverflow.model.UserWorkflowHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserWorkflowHistoryRepository extends JpaRepository<UserWorkflowHistory, Long> {
    
    List<UserWorkflowHistory> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<UserWorkflowHistory> findByWorkflowIdOrderByCreatedAtDesc(Long workflowId);
    
    List<UserWorkflowHistory> findByMindmapId(String mindmapId);
}

