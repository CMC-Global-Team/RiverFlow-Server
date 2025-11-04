package com.riverflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Cấu hình CORS (Cross-Origin Resource Sharing)
 * Cho phép client từ các domain khác nhau truy cập API
 */
@Configuration
public class WebConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Cho phép tất cả origins từ localhost với bất kỳ port nào
        // Bạn có thể thay đổi thành domain cụ thể khi deploy production
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:*",           // Cho phép localhost với mọi port
                "http://127.0.0.1:*",           // Cho phép 127.0.0.1 với mọi port
                "https://localhost:*",          // HTTPS localhost
                "https://127.0.0.1:*"           // HTTPS 127.0.0.1
        ));
        
        // Cho phép các HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
                "GET", 
                "POST", 
                "PUT", 
                "PATCH", 
                "DELETE", 
                "OPTIONS"
        ));
        
        // Cho phép tất cả headers
        configuration.setAllowedHeaders(List.of("*"));
        
        // Cho phép gửi credentials (cookies, authorization headers, etc.)
        configuration.setAllowCredentials(true);
        
        // Cho phép browser cache preflight request trong 1 giờ
        configuration.setMaxAge(3600L);
        
        // Expose các headers để client có thể đọc
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Total-Count"
        ));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}

