package com.riverflow.controller.mindmap;

import com.riverflow.dto.MessageResponse;
import com.riverflow.dto.mindmap.LogHistoryRequest;
import com.riverflow.model.mindmap.Mindmap;
import com.riverflow.model.mindmap.MindmapHistory;
import com.riverflow.repository.mindmap.MindmapRepository;
import com.riverflow.service.mindmap.MindmapHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/mindmaps")
@RequiredArgsConstructor
@Slf4j
public class MindmapHistoryController {

    private final MindmapHistoryService historyService;
    private final MindmapRepository mindmapRepository;

    @GetMapping("/{id}/history")
    public ResponseEntity<List<MindmapHistory>> getHistory(
            @PathVariable String id,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime after,
            @RequestParam(required = false, defaultValue = "100") Integer limit,
            Authentication authentication
    ) {
        Long userId = getUserId(authentication);
        log.info("Get history mindmapId={} userId={} action={} after={} limit={}", id, userId, action, after, limit);
        ensureCanView(id, userId);
        List<MindmapHistory> items = historyService.getHistory(id, action, after, limit);
        return ResponseEntity.ok(items);
    }

    @PostMapping("/{id}/history")
    public ResponseEntity<MessageResponse> logHistory(
            @PathVariable String id,
            @RequestBody LogHistoryRequest request,
            Authentication authentication
    ) {
        Long userId = getUserId(authentication);
        log.info("Log history mindmapId={} userId={} action={}", id, userId, request.getAction());
        ensureCanEdit(id, userId);
        historyService.logAction(
                id,
                userId,
                request.getAction(),
                request.getChanges(),
                request.getSnapshot(),
                request.getMetadata(),
                request.getStatus()
        );
        return ResponseEntity.ok(new MessageResponse("Logged"));
    }

    private Long getUserId(Authentication authentication) {
        return (authentication != null && authentication.getPrincipal() instanceof com.riverflow.config.jwt.UserPrincipal principal)
                ? principal.getId()
                : null;
    }

    private void ensureCanView(String mindmapId, Long userId) {
        Mindmap m = mindmapRepository.findById(mindmapId).orElse(null);
        if (m == null) {
            throw new com.riverflow.exception.mindmap.MindmapNotFoundException(mindmapId, userId);
        }
        if (Boolean.TRUE.equals(m.getIsPublic())) return;
        boolean collaborator = m.getCollaborators() != null && m.getCollaborators().stream()
                .anyMatch(c -> c.getMysqlUserId() != null && c.getMysqlUserId().equals(userId));
        Long ownerId = m.getMysqlUserId();
        boolean isOwner = ownerId != null && userId != null && ownerId.equals(userId);
        if (!isOwner && !collaborator) {
            throw new com.riverflow.exception.mindmap.MindmapAccessDeniedException("Không có quyền xem lịch sử.", mindmapId, userId);
        }
    }

    private void ensureCanEdit(String mindmapId, Long userId) {
        Mindmap m = mindmapRepository.findById(mindmapId).orElse(null);
        if (m == null) {
            throw new com.riverflow.exception.mindmap.MindmapNotFoundException(mindmapId, userId);
        }
        boolean publicEdit = Boolean.TRUE.equals(m.getIsPublic()) && "edit".equalsIgnoreCase(m.getPublicAccessLevel());
        boolean collaboratorEdit = m.getCollaborators() != null && m.getCollaborators().stream()
                .anyMatch(c -> c.getMysqlUserId() != null && c.getMysqlUserId().equals(userId));
        Long ownerId = m.getMysqlUserId();
        boolean isOwner = ownerId != null && userId != null && ownerId.equals(userId);
        if (!isOwner && !collaboratorEdit && !publicEdit) {
            throw new com.riverflow.exception.mindmap.MindmapAccessDeniedException("Không có quyền ghi lịch sử.", mindmapId, userId);
        }
    }
}
