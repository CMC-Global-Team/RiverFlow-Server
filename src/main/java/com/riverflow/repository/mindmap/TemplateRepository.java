package com.riverflow.repository.mindmap;

import com.riverflow.model.mindmap.Template;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TemplateRepository extends MongoRepository<Template, String> {
    
    List<Template> findByIsOfficialAndIsPublicAndStatus(Boolean isOfficial, Boolean isPublic, String status);
    
    List<Template> findByCategoryAndStatus(String category, String status);
    
    List<Template> findByCreatedByAndStatus(Long createdBy, String status);
    
    List<Template> findByAiWorkflowId(Long aiWorkflowId);
    
    List<Template> findTop10ByStatusOrderByUsageCountDesc(String status);
}

