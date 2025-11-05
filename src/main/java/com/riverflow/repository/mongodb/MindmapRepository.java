package com.riverflow.repository.mongodb;

import com.riverflow.model.mongodb.Mindmap;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MindmapRepository extends MongoRepository<Mindmap, String> {
    
    List<Mindmap> findByMysqlUserIdAndStatus(Long mysqlUserId, String status);
    
    List<Mindmap> findByMysqlUserIdAndStatusOrderByUpdatedAtDesc(Long mysqlUserId, String status);
    
    List<Mindmap> findByMysqlUserIdAndCategoryAndStatus(Long mysqlUserId, String category, String status);
    
    List<Mindmap> findByIsPublicAndStatus(Boolean isPublic, String status);
    
    Optional<Mindmap> findByShareToken(String shareToken);
    
    List<Mindmap> findByAiWorkflowId(Long aiWorkflowId);
    
    List<Mindmap> findByCollaboratorsMysqlUserIdAndStatus(Long mysqlUserId, String status);
}

