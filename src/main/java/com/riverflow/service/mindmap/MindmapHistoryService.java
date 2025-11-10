package com.riverflow.service.mindmap;

import com.riverflow.model.mindmap.MindmapHistory;
import com.riverflow.repository.mindmap.MindmapHistoryRepository;
import jakarta.transaction.Transactional;
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
}