package com.riverflow.config;

import com.riverflow.config.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;

    /**
     * Bean này cung cấp một trình mã hóa mật khẩu (BCrypt)
     * để chúng ta không lưu mật khẩu dạng văn bản thô.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Authentication Provider
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Authentication Manager
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
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

                // Cấu hình phân quyền cho các request
                .authorizeHttpRequests(authz -> authz
                        // Cho phép truy cập công khai vào các đường dẫn này
                        .requestMatchers(
                                "/api/auth/**",         // API xác thực
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
                )

                // Thêm Authentication Provider
                .authenticationProvider(authenticationProvider())

                // Thêm JWT Authentication Filter trước UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}