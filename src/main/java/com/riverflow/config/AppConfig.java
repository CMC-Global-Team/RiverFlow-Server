package com.riverflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Application Configuration
 * 
 * Note: JavaMailSender được auto-configure từ spring.mail.* properties
 * trong application.properties. Không cần tạo bean thủ công.
 */
@Configuration
public class AppConfig {
    
    /**
     * RestTemplate bean để gọi SMTP Server
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
