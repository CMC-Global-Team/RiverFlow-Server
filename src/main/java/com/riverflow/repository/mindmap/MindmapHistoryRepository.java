package com.riverflow.repository.mindmap;

import com.riverflow.model.mindmap.MindmapHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MindmapHistoryRepository extends MongoRepository<MindmapHistory, String> {
    
    List<MindmapHistory> findByMindmapIdOrderByCreatedAtDesc(String mindmapId);
    
    List<MindmapHistory> findByMysqlUserIdOrderByCreatedAtDesc(Long mysqlUserId);
    
    List<MindmapHistory> findByMindmapIdAndActionOrderByCreatedAtDesc(String mindmapId, String action);
    
    List<MindmapHistory> findByMindmapIdAndCreatedAtAfterOrderByCreatedAtDesc(String mindmapId, LocalDateTime after);

    Optional<MindmapHistory> findTopByMindmapIdAndMysqlUserIdOrderByCreatedAtDesc(
            String mindmapId,
            Long mysqlUserId
    );
}

