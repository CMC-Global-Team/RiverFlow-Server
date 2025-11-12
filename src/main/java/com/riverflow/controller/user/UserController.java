package com.riverflow.controller.user;

import com.riverflow.config.jwt.CustomUserDetailsService;
import com.riverflow.dto.authentication.UpdateUserRequest;
import com.riverflow.dto.authentication.UserResponse;
import com.riverflow.model.User;
import com.riverflow.service.user.AvatarService;
import com.riverflow.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for user profile operations
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final CustomUserDetailsService userDetailsService;
    private final AvatarService avatarService;

    /**
     * API Endpoint: Lấy thông tin người dùng
     * GET /api/user/profile
     */
    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getUserProfile(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        log.info("Getting profile for user: {}", userId);
        
        UserResponse response = userService.getUserById(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * API Endpoint: Cập nhật thông tin người dùng
     * PUT /api/user/profile
     */
    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateUserProfile(
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        log.info("Updating profile for user: {}", userId);
        
        UserResponse response = userService.updateUser(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * API Endpoint: Upload avatar image
     * POST /api/user/avatar/upload
     * Avatar is stored directly in database as BLOB
     */
    @PostMapping("/avatar/upload")
    public ResponseEntity<Map<String, String>> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            Long userId = getUserIdFromAuth(authentication);
            
            // Upload avatar to database
            avatarService.uploadAvatar(file, userId);
            
            log.info("Avatar uploaded for user: {}", userId);
            
            Map<String, String> response = new HashMap<>();
            response.put("url", "/api/user/avatar/" + userId);
            response.put("message", "Avatar uploaded successfully");
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid avatar upload: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error uploading avatar", e);
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to upload avatar: " + e.getMessage()));
        }
    }

    /**
     * API Endpoint: Get avatar image for user
     * GET /api/user/avatar/{userId}
     */
    @GetMapping("/avatar/{userId}")
    public ResponseEntity<?> getAvatar(@PathVariable Long userId) {
        try {
            var avatarOpt = avatarService.getAvatar(userId);
            
            if (avatarOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            var avatar = avatarOpt.get();
            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(avatar.getMimeType()))
                    .body(avatar.getData());
        } catch (Exception e) {
            log.error("Error retrieving avatar for user: {}", userId, e);
            return ResponseEntity.notFound().build();
        }
    }

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }

    /**
     * Helper method to get user ID from authentication
     */
    private Long getUserIdFromAuth(Authentication authentication) {
        String email = authentication.getName();
        User user = userDetailsService.loadUserEntityByEmail(email);
        return user.getId();
    }
}

