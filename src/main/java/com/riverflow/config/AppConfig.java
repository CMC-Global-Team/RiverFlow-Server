package com.riverflow.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Application Configuration
 * 
 * Note: JavaMailSender được auto-configure từ spring.mail.* properties
 * trong application.properties. Không cần tạo bean thủ công.
 */
@Configuration
@EnableAsync
public class AppConfig {
    // Bean configuration nếu cần trong tương lai
}
