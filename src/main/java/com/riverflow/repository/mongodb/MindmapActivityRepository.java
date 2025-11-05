package com.riverflow.repository.mongodb;

import com.riverflow.model.mongodb.MindmapActivity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MindmapActivityRepository extends MongoRepository<MindmapActivity, String> {
    
    List<MindmapActivity> findByMindmapIdOrderByCreatedAtDesc(String mindmapId);
    
    List<MindmapActivity> findByMysqlUserIdOrderByCreatedAtDesc(Long mysqlUserId);
    
    List<MindmapActivity> findByActivityTypeOrderByCreatedAtDesc(String activityType);
    
    List<MindmapActivity> findByMindmapIdAndCreatedAtAfterOrderByCreatedAtDesc(String mindmapId, LocalDateTime after);
}

