package com.riverflow.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Configuration for file storage and serving
 */
@Configuration
public class FileStorageConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadDir = "uploads/avatars";
        Path uploadPath = Paths.get(uploadDir);
        
        registry.addResourceHandler("/api/files/avatars/**")
                .addResourceLocations("file:" + uploadPath.toAbsolutePath().toString() + "/");
    }
}

