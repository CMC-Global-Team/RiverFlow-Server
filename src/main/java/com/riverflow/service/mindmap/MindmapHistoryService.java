package com.riverflow.service.mindmap;

import com.riverflow.model.mindmap.MindmapHistory;
import com.riverflow.repository.mindmap.MindmapHistoryRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
        String st = status != null ? status : "active";
        if ((changes == null || changes.isEmpty()) && (snapshot == null || snapshot.isEmpty())) {
            return null;
        }
        var last = historyRepository.findTopByMindmapIdAndMysqlUserIdAndStatusOrderByCreatedAtDesc(mindmapId, userId, st);
        if (last.isPresent()) {
            var prev = last.get();
            boolean sameAction = action != null && action.equals(prev.getAction());
            boolean sameChanges = java.util.Objects.equals(changes, prev.getChanges());
            boolean sameSnapshot = java.util.Objects.equals(snapshot, prev.getSnapshot());
            boolean withinWindow = prev.getCreatedAt() != null && prev.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(2));
            if (sameAction && sameChanges && sameSnapshot && withinWindow) {
                return null;
            }
        }
        MindmapHistory entry = MindmapHistory.builder()
                .mindmapId(mindmapId)
                .mysqlUserId(userId)
                .action(action)
                .changes(changes)
                .snapshot(snapshot)
                .metadata(metadata)
                .createdAt(LocalDateTime.now())
                .status(st)
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

    public Page<MindmapHistory> getHistoryPaged(
            String mindmapId,
            String action,
            LocalDateTime from,
            LocalDateTime to,
            Integer page,
            Integer size
    ) {
        int p = page != null && page >= 0 ? page : 0;
        int s = size != null && size > 0 ? size : 20;
        Pageable pageable = PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (action != null && !action.isBlank()) {
            return historyRepository.findByMindmapIdAndAction(mindmapId, action, pageable);
        }
        if (from != null && to != null) {
            return historyRepository.findByMindmapIdAndCreatedAtBetween(mindmapId, from, to, pageable);
        }
        return historyRepository.findByMindmapId(mindmapId, pageable);
    }
}
