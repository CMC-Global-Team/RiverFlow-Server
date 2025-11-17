package com.riverflow.service;

/**
 * Interface for SMTP Email Service
 * Gửi email thông qua SMTP Proxy Server
 */
public interface SmtpEmailService {
    
    /**
     * Gửi email xác thực tài khoản
     * @param to Email người nhận
     * @param token Token xác thực
     */
    void sendVerificationEmail(String to, String token);
    
    /**
     * Gửi email reset password
     * @param to Email người nhận
     * @param token Token reset password
     */
    void sendResetPasswordEmail(String to, String token);
    
    /**
     * Gửi email chung
     * @param to Email người nhận
     * @param subject Chủ đề email
     * @param html Nội dung HTML
     * @param text Nội dung text (optional)
     */
    void sendEmail(String to, String subject, String html, String text);

    /**
     * Gửi email mời cộng tác
     * @param to Email người nhận
     * @param token Token lời mời
     * @param inviterName Tên người mời
     * @param mindmapTitle Tên mindmap được chia sẻ
     */
    void sendInvitationEmail(String to, String token, String inviterName, String mindmapTitle);
}

