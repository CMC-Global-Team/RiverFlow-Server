package com.riverflow.repository.mongodb;

import com.riverflow.model.mongodb.Comment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends MongoRepository<Comment, String> {
    
    List<Comment> findByMindmapIdOrderByCreatedAtDesc(String mindmapId);
    
    List<Comment> findByMindmapIdAndNodeIdOrderByCreatedAtDesc(String mindmapId, String nodeId);
    
    List<Comment> findByMysqlUserId(Long mysqlUserId);
    
    List<Comment> findByMindmapIdAndResolved(String mindmapId, Boolean resolved);
    
    List<Comment> findByMentionsContaining(Long mysqlUserId);
}

