package com.riverflow.service;

public interface EmailService {

    /**
     * Gửi một email văn bản đơn giản.
     * @param to Địa chỉ email người nhận
     * @param subject Chủ đề email
     * @param body Nội dung email
     */
    void sendSimpleMessage(String to, String subject, String body);
}