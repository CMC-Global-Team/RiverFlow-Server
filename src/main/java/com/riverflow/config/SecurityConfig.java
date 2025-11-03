package com.riverflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Bean này cung cấp một trình mã hóa mật khẩu (BCrypt)
     * để chúng ta không lưu mật khẩu dạng văn bản thô.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Cache-Control"));

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Áp dụng cấu hình này cho tất cả các đường dẫn
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    /**
     * Cấu hình chuỗi lọc bảo mật chính.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Bật CORS (sẽ được cấu hình chi tiết trong WebConfig)
                .cors(withDefaults())

                // Tắt CSRF (Cross-Site Request Forgery) vì chúng ta dùng API stateless
                .csrf(csrf -> csrf.disable())

                // Tắt các cơ chế xác thực mặc định (form và basic)
                .formLogin(form -> form.disable())
                .httpBasic(httpBasic -> httpBasic.disable())

                // Cấu hình phân quyền cho các request
                .authorizeHttpRequests(authz -> authz
                        // Cho phép truy cập công khai vào các đường dẫn này
                        .requestMatchers(
                                "/auth/**",         // API xác thực
                                "/swagger-ui.html",     // Trang UI Swagger
                                "/swagger-ui/**",       // Tài nguyên của Swagger
                                "/v3/api-docs/**"       // File JSON định nghĩa OpenAPI
                        ).permitAll()

                        // Bất kỳ request nào khác đều yêu cầu phải xác thực
                        .anyRequest().authenticated()
                )

                // Cấu hình session: không sử dụng (stateless)
                // Chúng ta sẽ dùng JWT thay vì session
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        return http.build();
    }
}