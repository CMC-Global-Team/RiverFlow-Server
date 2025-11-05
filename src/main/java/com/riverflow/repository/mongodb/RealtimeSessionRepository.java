package com.riverflow.repository.mongodb;

import com.riverflow.model.mongodb.RealtimeSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RealtimeSessionRepository extends MongoRepository<RealtimeSession, String> {
    
    List<RealtimeSession> findByMindmapIdAndIsActive(String mindmapId, Boolean isActive);
    
    Optional<RealtimeSession> findBySocketId(String socketId);
    
    List<RealtimeSession> findByMysqlUserId(Long mysqlUserId);
    
    void deleteByLastActivityBefore(LocalDateTime before);
}

