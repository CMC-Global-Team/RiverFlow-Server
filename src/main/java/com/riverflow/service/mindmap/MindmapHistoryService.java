package com.riverflow.service.mindmap;

import com.riverflow.model.mindmap.MindmapHistory;
import com.riverflow.repository.mindmap.MindmapHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MindmapHistoryService {

    private final MindmapHistoryRepository historyRepository;

    /**
     * Ghi lại một thay đổi vào collection mindmap_history.
     *
     * @param mindmapId ID của mindmap bị thay đổi
     * @param userId ID (từ MySQL) của người thực hiện
     * @param action Loại hành động (ví dụ: "node_add", "node_update")
     * @param before Trạng thái của đối tượng (ví dụ: Node, Edge) TRƯỚC khi thay đổi
     * @param after Trạng thái của đối tượng (ví dụ: Node, Edge) SAU khi thay đổi
     */
    public void recordChange(String mindmapId, Long userId, String action, Object before, Object after) {

        // Tạo một Map để lưu trữ trạng thái 'before' và 'after'
        Map<String, Object> changes = new HashMap<>();
        changes.put("before", before); // Lưu trạng thái cũ (quan trọng cho Undo)
        changes.put("after", after);   // Lưu trạng thái mới (quan trọng cho Redo)

        // Tạo bản ghi lịch sử
        MindmapHistory historyEntry = MindmapHistory.builder()
                .mindmapId(mindmapId)
                .mysqlUserId(userId)
                .action(action)
                .changes(changes)
                .createdAt(LocalDateTime.now()) // Dùng thời gian hiện tại
                .build();

        // Lưu vào MongoDB
        historyRepository.save(historyEntry);
    }
}