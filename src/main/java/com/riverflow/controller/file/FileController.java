package com.riverflow.controller.file;

import com.riverflow.service.user.AvatarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for serving uploaded files from database
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final AvatarService avatarService;

    /**
     * Serve avatar files from database
     * GET /api/files/avatars/{userId}
     * 
     * Note: Avatar endpoint moved to /api/user/avatar/{userId}
     * This endpoint is kept for backward compatibility
     */
    @GetMapping("/avatars/{userId}")
    public ResponseEntity<?> serveAvatar(@PathVariable Long userId) {
        try {
            var avatarOpt = avatarService.getAvatar(userId);
            
            if (avatarOpt.isEmpty()) {
                log.debug("Avatar not found for user: {}", userId);
                return ResponseEntity.notFound().build();
            }
            
            var avatar = avatarOpt.get();
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(avatar.getMimeType()))
                    .body(avatar.getData());
        } catch (Exception e) {
            log.error("Error serving avatar for user: {}", userId, e);
            return ResponseEntity.notFound().build();
        }
    }
}


