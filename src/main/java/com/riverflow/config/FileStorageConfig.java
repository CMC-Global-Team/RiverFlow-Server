package com.riverflow.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration for file storage and serving
 * 
 * NOTE: Not using Spring's ResourceHandler for /api/files/avatars/**
 * because it throws 500 errors when files are missing.
 * Instead, FileController handles file serving with graceful 404 responses.
 */
@Configuration
@Slf4j
public class FileStorageConfig implements WebMvcConfigurer {

    /**
     * FileController handles all file serving for /api/files/avatars/**
     * This provides better error handling than Spring's built-in ResourceHandler
     */
    // Resource handler intentionally disabled to allow FileController to handle gracefully
}

