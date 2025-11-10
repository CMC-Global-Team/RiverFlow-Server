package com.riverflow.service;

import com.riverflow.dto.smtp.SmtpEmailRequest;
import com.riverflow.dto.smtp.SmtpEmailResponse;
import com.riverflow.dto.smtp.SmtpResetPasswordEmailRequest;
import com.riverflow.dto.smtp.SmtpVerificationEmailRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation of SMTP Email Service
 * Gọi SMTP Proxy Server để gửi email
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SmtpEmailServiceImpl implements SmtpEmailService {

    private final RestTemplate restTemplate;

    @Value("${app.smtp.server.url:http://localhost:3001}")
    private String smtpServerUrl;

    @Value("${app.smtp.server.api-key:riverflow-smtp-secure-key-2024}")
    private String apiKey;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    /**
     * Tạo HTTP headers với API key
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", apiKey);
        return headers;
    }

    @Override
    public void sendVerificationEmail(String to, String token) {
        try {
            SmtpVerificationEmailRequest request = SmtpVerificationEmailRequest.builder()
                    .to(to)
                    .token(token)
                    .frontendUrl(frontendUrl)
                    .build();

            HttpEntity<SmtpVerificationEmailRequest> entity = new HttpEntity<>(request, createHeaders());
            String url = smtpServerUrl + "/api/email/verification";

            SmtpEmailResponse response = restTemplate.postForObject(url, entity, SmtpEmailResponse.class);
            
            if (response != null && response.getSuccess()) {
                log.info("Verification email sent successfully to {} via SMTP server", to);
            } else {
                log.error("Failed to send verification email to {}: {}", to, 
                    response != null ? response.getMessage() : "No response");
                throw new RuntimeException("Failed to send verification email");
            }
        } catch (Exception e) {
            log.error("Error sending verification email to {} via SMTP server: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send verification email: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendResetPasswordEmail(String to, String token) {
        try {
            SmtpResetPasswordEmailRequest request = SmtpResetPasswordEmailRequest.builder()
                    .to(to)
                    .token(token)
                    .frontendUrl(frontendUrl)
                    .build();

            HttpEntity<SmtpResetPasswordEmailRequest> entity = new HttpEntity<>(request, createHeaders());
            String url = smtpServerUrl + "/api/email/reset-password";

            SmtpEmailResponse response = restTemplate.postForObject(url, entity, SmtpEmailResponse.class);
            
            if (response != null && response.getSuccess()) {
                log.info("Reset password email sent successfully to {} via SMTP server", to);
            } else {
                log.error("Failed to send reset password email to {}: {}", to, 
                    response != null ? response.getMessage() : "No response");
                throw new RuntimeException("Failed to send reset password email");
            }
        } catch (Exception e) {
            log.error("Error sending reset password email to {} via SMTP server: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send reset password email: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendEmail(String to, String subject, String html, String text) {
        try {
            SmtpEmailRequest request = SmtpEmailRequest.builder()
                    .to(to)
                    .subject(subject)
                    .html(html)
                    .text(text)
                    .build();

            HttpEntity<SmtpEmailRequest> entity = new HttpEntity<>(request, createHeaders());
            String url = smtpServerUrl + "/api/email/send";

            SmtpEmailResponse response = restTemplate.postForObject(url, entity, SmtpEmailResponse.class);
            
            if (response != null && response.getSuccess()) {
                log.info("Email sent successfully to {} via SMTP server", to);
            } else {
                log.error("Failed to send email to {}: {}", to, 
                    response != null ? response.getMessage() : "No response");
                throw new RuntimeException("Failed to send email");
            }
        } catch (Exception e) {
            log.error("Error sending email to {} via SMTP server: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }
}

