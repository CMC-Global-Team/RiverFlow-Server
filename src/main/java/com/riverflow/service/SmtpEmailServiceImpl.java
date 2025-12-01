package com.riverflow.service;

import com.riverflow.dto.smtp.*;
import lombok.RequiredArgsConstructor;
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
            // Normalize frontend URL (remove trailing slash if present)
            String normalizedFrontendUrl = frontendUrl != null 
                ? frontendUrl.trim().replaceAll("/+$", "") 
                : frontendUrl;
            
            SmtpVerificationEmailRequest request = SmtpVerificationEmailRequest.builder()
                    .to(to)
                    .token(token)
                    .frontendUrl(normalizedFrontendUrl)
                    .build();

            HttpEntity<SmtpVerificationEmailRequest> entity = new HttpEntity<>(request, createHeaders());
            String url = smtpServerUrl + "/api/email/verification";

            SmtpEmailResponse response = restTemplate.postForObject(url, entity, SmtpEmailResponse.class);
            
            if (response != null && response.getSuccess()) {
                } else {
                throw new RuntimeException("Failed to send verification email");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to send verification email: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendResetPasswordEmail(String to, String token) {
        try {
            // Normalize frontend URL (remove trailing slash if present)
            String normalizedFrontendUrl = frontendUrl != null 
                ? frontendUrl.trim().replaceAll("/+$", "") 
                : frontendUrl;
            
            SmtpResetPasswordEmailRequest request = SmtpResetPasswordEmailRequest.builder()
                    .to(to)
                    .token(token)
                    .frontendUrl(normalizedFrontendUrl)
                    .build();

            HttpEntity<SmtpResetPasswordEmailRequest> entity = new HttpEntity<>(request, createHeaders());
            String url = smtpServerUrl + "/api/email/reset-password";

            SmtpEmailResponse response = restTemplate.postForObject(url, entity, SmtpEmailResponse.class);
            
            if (response != null && response.getSuccess()) {
                } else {
                throw new RuntimeException("Failed to send reset password email");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to send reset password email: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendInvitationEmail(String to, String token, String inviterName, String mindmapTitle) {
        try {
            String normalizedFrontendUrl = frontendUrl != null
                    ? frontendUrl.trim().replaceAll("/+$", "")
                    : frontendUrl;

            SmtpInvitationEmailRequest request = SmtpInvitationEmailRequest.builder()
                    .to(to)
                    .token(token)
                    .inviterName(inviterName)
                    .mindmapTitle(mindmapTitle)
                    .frontendUrl(normalizedFrontendUrl)
                    .build();

            HttpEntity<SmtpInvitationEmailRequest> entity = new HttpEntity<>(request, createHeaders());
            String url = smtpServerUrl + "/api/email/invitation";

            SmtpEmailResponse response = restTemplate.postForObject(url, entity, SmtpEmailResponse.class);

            if (response != null && response.getSuccess()) {
                } else {
                }
        } catch (Exception e) {
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
                } else {
                throw new RuntimeException("Failed to send email");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }
}

