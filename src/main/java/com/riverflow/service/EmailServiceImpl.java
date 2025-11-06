package com.riverflow.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);
    
    private final JavaMailSender mailSender;

    // Tự động đọc giá trị 'spring.mail.from' từ application.properties
    @Value("${spring.mail.from}")
    private String fromEmail;
    
    // Email enabled flag (false on Render free tier due to SMTP block)
    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;

    @Autowired
    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    @Async
    public void sendSimpleMessage(String to, String subject, String body) {
        // Skip email sending if disabled (e.g., on Render free tier)
        if (!emailEnabled) {
            logger.warn("Email sending is disabled. Skipping email to: {}", to);
            logger.info("Email would have been sent - Subject: {}", subject);
            return;
        }
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            
            mailSender.send(message);
            logger.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            // Log error but don't throw exception to avoid breaking registration
            logger.error("Failed to send email to: {}. Error: {}", to, e.getMessage());
            logger.warn("User registration will proceed without email verification");
        }
    }
}