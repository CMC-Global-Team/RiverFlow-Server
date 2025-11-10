package com.riverflow.service.mindmap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riverflow.dto.mindmap.MindmapResponse;
import com.riverflow.exception.mindmap.MindmapNotFoundException;
import com.riverflow.model.mindmap.Mindmap;
import com.riverflow.model.mindmap.MindmapHistory;
import com.riverflow.repository.mindmap.MindmapHistoryRepository;
import com.riverflow.repository.mindmap.MindmapRepository;
import com.riverflow.util.mindmap.MindmapMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UndoRedoService {

    private final MindmapRepository mindmapRepository;
    private final MindmapHistoryRepository historyRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public MindmapResponse undo(String mindmapId, Long userId) {
        log.info("Undo cho mindmap: {} bởi user: {}", mindmapId, userId);

        MindmapHistory lastAction = historyRepository
                .findTopByMindmapIdAndMysqlUserIdAndStatusOrderByCreatedAtDesc(mindmapId, userId, "active")
                .orElseThrow(() -> new RuntimeException("Không có gì để hoàn tác"));

        Mindmap mindmap = applyChanges(mindmapId, userId, lastAction, "before");

        lastAction.setStatus("undone");
        lastAction.setCreatedAt(LocalDateTime.now());
        historyRepository.save(lastAction);

        log.info("Undo thành công hành động: {}", lastAction.getAction());
        return MindmapMapper.toResponse(mindmap);
    }

    @Transactional
    public MindmapResponse redo(String mindmapId, Long userId) {
        log.info("Redo cho mindmap: {} bởi user: {}", mindmapId, userId);

        MindmapHistory lastUndoneAction = historyRepository
                .findTopByMindmapIdAndMysqlUserIdAndStatusOrderByCreatedAtDesc(mindmapId, userId, "undone")
                .orElseThrow(() -> new RuntimeException("Không có gì để làm lại"));

        Mindmap mindmap = applyChanges(mindmapId, userId, lastUndoneAction, "after");

        lastUndoneAction.setStatus("active");
        lastUndoneAction.setCreatedAt(LocalDateTime.now());
        historyRepository.save(lastUndoneAction);

        log.info("Redo thành công hành động: {}", lastUndoneAction.getAction());
        return MindmapMapper.toResponse(mindmap);
    }

    /**
     * Áp dụng thay đổi (khôi phục 'before' hoặc 'after')
     */
    private Mindmap applyChanges(String mindmapId, Long userId, MindmapHistory action, String state) {
        Mindmap mindmap = mindmapRepository.findById(mindmapId).orElse(null);

        switch (action.getAction()) {
            case "create_mindmap":
                if (state.equals("before")) { // Undo "create"
                    log.debug("Hoàn tác [create_mindmap]: Xóa mindmap {}", mindmapId);
                    if (mindmap != null) mindmapRepository.delete(mindmap);
                    return null;
                } else { // Redo "create"
                    log.debug("Làm lại [create_mindmap]: Tạo lại mindmap {}", mindmapId);
                    Object afterState = action.getChanges().get("after");
                    Mindmap restoredMindmap = objectMapper.convertValue(afterState, Mindmap.class);
                    return mindmapRepository.save(restoredMindmap);
                }

            case "update_mindmap":
                log.debug("Áp dụng [update_mindmap] - khôi phục state '{}'", state);
                if (mindmap == null) throw new MindmapNotFoundException(mindmapId, userId);

                Object snapshot = action.getChanges().get(state);
                if (snapshot == null) throw new RuntimeException("Lịch sử hỏng: thiếu snapshot '" + state + "'");

                Mindmap restoredState = objectMapper.convertValue(snapshot, Mindmap.class);
                return mindmapRepository.save(applyRestoredState(mindmap, restoredState));

            case "toggle_favorite":
            case "archive_mindmap":
            case "unarchive_mindmap":
            case "delete_mindmap":
                log.debug("Áp dụng [{}] - khôi phục state '{}'", action.getAction(), state);
                if (mindmap == null) throw new MindmapNotFoundException(mindmapId, userId);

                Object simpleState = action.getChanges().get(state);
                return mindmapRepository.save(applySimpleChange(mindmap, action.getAction(), simpleState));

            default:
                log.warn("Hành động không được hỗ trợ: {}", action.getAction());
                throw new UnsupportedOperationException("Hành động không được hỗ trợ: " + action.getAction());
        }
    }

    /**
     * Helper: Áp dụng khôi phục cho các thay đổi snapshot (chunky update)
     */
    private Mindmap applyRestoredState(Mindmap current, Mindmap restored) {
        current.setTitle(restored.getTitle());
        current.setDescription(restored.getDescription());
        current.setThumbnail(restored.getThumbnail());
        current.setNodes(restored.getNodes());
        current.setEdges(restored.getEdges());
        current.setViewport(restored.getViewport());
        current.setSettings(restored.getSettings());
        current.setIsPublic(restored.getIsPublic());
        current.setShareToken(restored.getShareToken());
        current.setCollaborators(restored.getCollaborators());
        current.setTags(restored.getTags());
        current.setCategory(restored.getCategory());
        current.setIsFavorite(restored.getIsFavorite());
        current.setIsTemplate(restored.getIsTemplate());
        current.setStatus(restored.getStatus());
        current.setUpdatedAt(LocalDateTime.now());
        return current;
    }

    /**
     * Helper: Áp dụng khôi phục cho các thay đổi đơn lẻ
     */
    private Mindmap applySimpleChange(Mindmap mindmap, String action, Object stateToApply) {
        if (stateToApply == null) {
            throw new RuntimeException("Lịch sử hỏng: thiếu state cho hành động " + action);
        }

        switch (action) {
            case "toggle_favorite":
                mindmap.setIsFavorite((Boolean) stateToApply);
                break;
            case "archive_mindmap":
            case "unarchive_mindmap":
            case "delete_mindmap":
                mindmap.setStatus(stateToApply.toString());
                break;
        }

        mindmap.setUpdatedAt(LocalDateTime.now());
        return mindmap;
    }
}