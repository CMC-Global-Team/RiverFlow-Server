package com.riverflow.config;

import com.riverflow.service.user.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * Configuration for file storage and serving
 */
@Configuration
@RequiredArgsConstructor
public class FileStorageConfig implements WebMvcConfigurer {

    private final FileStorageService fileStorageService;

    // Static resource handler removed - FileController now handles file serving
    // This prevents conflicts between static resource handler and controller endpoints
    // @Override
    // public void addResourceHandlers(ResourceHandlerRegistry registry) {
    //     // Removed - using FileController instead
    // }
}

