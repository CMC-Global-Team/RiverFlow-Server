package com.riverflow.service.mindmap;

import com.riverflow.model.mindmap.MindmapHistory;
import com.riverflow.repository.mindmap.MindmapHistoryRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MindmapHistoryService {

    private final MindmapHistoryRepository historyRepository;

    @Transactional
    public void recordChange(String mindmapId, Long userId, String action, Object before, Object after) {

        historyRepository.deleteAllByMindmapIdAndMysqlUserIdAndStatus(
                mindmapId,
                userId,
                "undone"
        );

        Map<String, Object> changes = new HashMap<>();
        changes.put("before", before);
        changes.put("after", after);

        MindmapHistory historyEntry = MindmapHistory.builder()
                .mindmapId(mindmapId)
                .mysqlUserId(userId)
                .action(action)
                .changes(changes)
                .createdAt(LocalDateTime.now())
                .status("active")
                .build();

        historyRepository.save(historyEntry);
    }

    @Transactional
    public MindmapHistory logAction(
            String mindmapId,
            Long userId,
            String action,
            Map<String, Object> changes,
            Map<String, Object> snapshot,
            Map<String, Object> metadata,
            String status
    ) {
        MindmapHistory entry = MindmapHistory.builder()
                .mindmapId(mindmapId)
                .mysqlUserId(userId)
                .action(action)
                .changes(changes)
                .snapshot(snapshot)
                .metadata(metadata)
                .createdAt(LocalDateTime.now())
                .status(status != null ? status : "active")
                .build();
        return historyRepository.save(entry);
    }

    public java.util.List<MindmapHistory> getHistory(String mindmapId, String action, LocalDateTime after, Integer limit) {
        if (action != null && !action.isBlank()) {
            var list = historyRepository.findByMindmapIdAndActionOrderByCreatedAtDesc(mindmapId, action);
            return (limit != null && limit > 0 && list.size() > limit) ? list.subList(0, limit) : list;
        }
        if (after != null) {
            var list = historyRepository.findByMindmapIdAndCreatedAtAfterOrderByCreatedAtDesc(mindmapId, after);
            return (limit != null && limit > 0 && list.size() > limit) ? list.subList(0, limit) : list;
        }
        var list = historyRepository.findByMindmapIdOrderByCreatedAtDesc(mindmapId);
        return (limit != null && limit > 0 && list.size() > limit) ? list.subList(0, limit) : list;
    }
}
