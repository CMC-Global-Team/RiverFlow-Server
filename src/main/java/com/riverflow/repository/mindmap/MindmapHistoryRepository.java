package com.riverflow.repository.mindmap;

import com.riverflow.model.mindmap.MindmapHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    Page<MindmapHistory> findByMindmapId(String mindmapId, Pageable pageable);
    Page<MindmapHistory> findByMindmapIdAndAction(String mindmapId, String action, Pageable pageable);
    Page<MindmapHistory> findByMindmapIdAndCreatedAtBetween(String mindmapId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    Optional<MindmapHistory> findTopByMindmapIdAndMysqlUserIdAndStatusOrderByCreatedAtDesc(
            String mindmapId,
            Long mysqlUserId,
            String status
    );

    /**
     * (Mới cho việc clear redo stack)
     * Xóa tất cả lịch sử "undone" của user trên mindmap
     */
    void deleteAllByMindmapIdAndMysqlUserIdAndStatus(
            String mindmapId,
            Long mysqlUserId,
            String status
    );
}

