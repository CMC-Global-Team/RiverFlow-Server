package com.riverflow.controller.file;

import com.riverflow.service.user.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Controller for serving uploaded files
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final FileStorageService fileStorageService;

    /**
     * Serve avatar files
     * GET /api/files/avatars/{filename}
     */
    @GetMapping("/avatars/{filename:.+}")
    public ResponseEntity<Resource> serveAvatar(@PathVariable String filename) {
        try {
            Path uploadPath = fileStorageService.getResolvedUploadPath();
            Path filePath = uploadPath.resolve(filename).normalize();
            
            // Security check: ensure the file is within the upload directory
            if (!filePath.startsWith(uploadPath.normalize())) {
                log.warn("Attempted path traversal attack: {}", filename);
                return ResponseEntity.notFound().build();
            }
            
            File file = filePath.toFile();
            if (!file.exists() || !file.isFile()) {
                log.warn("Avatar file not found: {}", filePath);
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new FileSystemResource(file);
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (Exception e) {
            log.error("Error serving avatar file: {}", filename, e);
            return ResponseEntity.notFound().build();
        }
    }
}

