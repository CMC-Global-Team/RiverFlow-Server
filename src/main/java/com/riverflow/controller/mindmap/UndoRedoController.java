package com.riverflow.controller.mindmap;

import com.riverflow.dto.mindmap.MindmapResponse;
import com.riverflow.service.mindmap.UndoRedoService;
import com.riverflow.config.jwt.UserPrincipal;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UndoRedoController {

    private final UndoRedoService undoRedoService;

    /**
     * API Endpoint: Hoàn tác (Undo)
     * POST /api/auth/mindmaps/{mapId}/undo
     */
    @PostMapping("/auth/mindmaps/{mapId}/undo")
    public ResponseEntity<MindmapResponse> undo(
            @PathVariable String mapId,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        Long userId = currentUser.getId();

        MindmapResponse response = undoRedoService.undo(mapId, userId);

        return ResponseEntity.ok(response);
    }

    /**
     * API Endpoint: Làm lại (Redo)
     * POST /api/auth/mindmaps/{mapId}/redo
     */
    @PostMapping("/auth/mindmaps/{mapId}/redo")
    public ResponseEntity<MindmapResponse> redo(
            @PathVariable String mapId,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        Long userId = currentUser.getId();

        MindmapResponse response = undoRedoService.redo(mapId, userId);

        return ResponseEntity.ok(response);
    }
}