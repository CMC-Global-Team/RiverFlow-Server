package com.riverflow.service.user;

import com.riverflow.model.User;
import com.riverflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

/**
 * Service for handling avatar storage and retrieval from database
 * Avatars are stored as BLOB in MySQL instead of on disk
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AvatarService {

    private final UserRepository userRepository;
    
    private static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String[] ALLOWED_MIME_TYPES = {
        "image/jpeg",
        "image/png",
        "image/webp",
        "image/gif"
    };

    /**
     * Upload and store avatar for user
     * @param file The avatar file
     * @param userId The user ID
     * @throws IOException if file reading fails
     * @throws IllegalArgumentException if file validation fails
     */
    public void uploadAvatar(MultipartFile file, Long userId) throws IOException {
        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new IllegalArgumentException("File size exceeds 5MB limit");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !isAllowedMimeType(contentType)) {
            throw new IllegalArgumentException("Only image files (JPEG, PNG, WebP, GIF) are allowed");
        }
        
        // Get user and update avatar
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        byte[] avatarData = file.getBytes();
        user.setAvatarData(avatarData);
        user.setAvatarMimeType(contentType);
        user.setAvatar(null); // Clear old URL-based avatar
        
        userRepository.save(user);
        log.info("Avatar uploaded for user: {}, size: {} bytes", userId, avatarData.length);
    }

    /**
     * Get avatar data for user
     * @param userId The user ID
     * @return Optional containing avatar data
     */
    public Optional<AvatarData> getAvatar(Long userId) {
        return userRepository.findById(userId)
                .filter(user -> user.getAvatarData() != null && user.getAvatarData().length > 0)
                .map(user -> new AvatarData(
                    user.getAvatarData(),
                    user.getAvatarMimeType()
                ));
    }

    /**
     * Delete avatar for user
     * @param userId The user ID
     */
    public void deleteAvatar(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        user.setAvatarData(null);
        user.setAvatarMimeType(null);
        user.setAvatar(null);
        
        userRepository.save(user);
        log.info("Avatar deleted for user: {}", userId);
    }

    /**
     * Check if MIME type is allowed
     */
    private boolean isAllowedMimeType(String mimeType) {
        for (String allowed : ALLOWED_MIME_TYPES) {
            if (mimeType.equalsIgnoreCase(allowed)) {
                return true;
            }
        }
        return false;
    }

    /**
     * DTO for avatar data
     */
    public static class AvatarData {
        private final byte[] data;
        private final String mimeType;

        public AvatarData(byte[] data, String mimeType) {
            this.data = data;
            this.mimeType = mimeType;
        }

        public byte[] getData() {
            return data;
        }

        public String getMimeType() {
            return mimeType;
        }
    }
}
