package com.riverflow.service.user;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Service for handling file storage operations
 */
@Service
@Slf4j
public class FileStorageService {

    @Value("${file.upload-dir:uploads/avatars}")
    private String uploadDir;

    @Value("${app.backend.url:http://localhost:8080/api}")
    private String backendUrl;

    private static final String FALLBACK_TMP_DIR = "riverflow";

    private Path resolvedUploadPath;

    @PostConstruct
    void init() {
        this.resolvedUploadPath = initialiseUploadPath();
        log.info("Avatar uploads directory set to {}", resolvedUploadPath);
    }

    /**
     * Store uploaded file and return the URL
     */
    public String storeFile(MultipartFile file, Long userId) throws IOException {
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = "avatar_" + userId + "_" + UUID.randomUUID().toString() + extension;

        // Save file
        Path filePath = resolvedUploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        log.info("File saved: {}", filePath);

        // Return URL - use backend URL from config
        return backendUrl + "/files/avatars/" + filename;
    }

    /**
     * Delete file
     */
    public void deleteFile(String filename) throws IOException {
        Path filePath = resolvedUploadPath.resolve(filename);
        if (Files.exists(filePath)) {
            Files.delete(filePath);
            log.info("File deleted: {}", filePath);
        }
    }

    private Path initialiseUploadPath() {
        Path configuredPath = Paths.get(uploadDir);
        if (!configuredPath.isAbsolute()) {
            Path tmpBase = Paths.get(System.getProperty("java.io.tmpdir"))
                    .resolve(FALLBACK_TMP_DIR);
            configuredPath = tmpBase.resolve(uploadDir).normalize();
        }

        configuredPath = configuredPath.toAbsolutePath().normalize();

        try {
            Files.createDirectories(configuredPath);
            return configuredPath;
        } catch (AccessDeniedException ex) {
            Path fallbackPath = Paths.get(System.getProperty("java.io.tmpdir"))
                    .resolve(FALLBACK_TMP_DIR)
                    .resolve("uploads")
                    .resolve("avatars")
                    .toAbsolutePath()
                    .normalize();

            try {
                Files.createDirectories(fallbackPath);
                log.warn("Access denied for avatar upload dir '{}'. Falling back to '{}'.",
                        configuredPath, fallbackPath);
                return fallbackPath;
            } catch (IOException fallbackError) {
                throw new IllegalStateException(
                        "Failed to create fallback avatar upload directory at " + fallbackPath,
                        fallbackError
                );
            }
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Failed to initialise avatar upload directory at " + configuredPath,
                    ex
            );
        }
    }

    public Path getResolvedUploadPath() {
        return resolvedUploadPath;
    }
}

