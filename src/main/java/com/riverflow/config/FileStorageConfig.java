package com.riverflow.config;

import com.riverflow.service.user.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * Configuration for file storage and serving
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class FileStorageConfig implements WebMvcConfigurer {

    private final FileStorageService fileStorageService;

    /**
     * Configure resource handlers for serving uploaded files
     * Maps /api/files/avatars/** to the actual upload directory on disk
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        try {
            Path uploadPath = fileStorageService.getResolvedUploadPath();
            String uploadDir = uploadPath.toUri().toString();
            
            log.info("Configuring resource handler for: {} -> {}", "/api/files/avatars/", uploadDir);
            
            registry.addResourceHandler("/api/files/avatars/**")
                    .addResourceLocations(uploadDir)
                    .setCachePeriod(3600) // Cache for 1 hour
                    .resourceChain(true)
                    .addResolver(new org.springframework.web.servlet.resource.PathResourceResolver());
        } catch (Exception e) {
            log.warn("Failed to configure resource handler for avatars: {}", e.getMessage());
            // Gracefully continue - FileController will handle serving
        }
    }
}

