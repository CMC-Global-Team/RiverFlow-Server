package com.riverflow.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    // Tự động đọc giá trị 'spring.mail.from' từ application.properties
    @Value("${spring.mail.from}")
    private String fromEmail;

    @Autowired
    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendSimpleMessage(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();

            // Lấy email "Người gửi" đã cấu hình
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            // Gửi mail
            mailSender.send(message);

        } catch (Exception e) {
            // Nếu gửi thất bại (ví dụ: Brevo sập), nó sẽ in lỗi ra console
            // nhưng không làm sập toàn bộ luồng đăng ký
            System.err.println("Lỗi khi gửi email: " + e.getMessage());
        }
    }
}