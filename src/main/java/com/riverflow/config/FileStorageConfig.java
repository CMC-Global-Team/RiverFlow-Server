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

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = fileStorageService.getResolvedUploadPath();
        String resourceLocation = uploadPath.toUri().toString();
        if (!resourceLocation.endsWith("/")) {
            resourceLocation = resourceLocation + "/";
        }

        // Note: Do NOT include the context-path (/api) here; Spring applies it automatically.
        registry.addResourceHandler("/files/avatars/**")
                .addResourceLocations(resourceLocation);
    }
}

