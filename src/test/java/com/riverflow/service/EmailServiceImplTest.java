package com.riverflow.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for EmailServiceImpl using Mockito
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@riverflow.com");
    }

    @Test
    void sendSimpleMessage_ValidParameters_SendsEmail() {
        // Given
        String to = "recipient@example.com";
        String subject = "Test Subject";
        String body = "Test Body";

        // When
        emailService.sendSimpleMessage(to, subject, body);

        // Then
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getFrom()).isEqualTo("noreply@riverflow.com");
        assertThat(sentMessage.getTo()).containsExactly(to);
        assertThat(sentMessage.getSubject()).isEqualTo(subject);
        assertThat(sentMessage.getText()).isEqualTo(body);
    }

    @Test
    void sendSimpleMessage_MultipleRecipients_SendsToCorrectAddress() {
        // Given
        String to = "user@example.com";
        String subject = "Welcome";
        String body = "Welcome to RiverFlow!";

        // When
        emailService.sendSimpleMessage(to, subject, body);

        // Then
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage.getTo()).containsExactly("user@example.com");
    }

    @Test
    void sendSimpleMessage_CallsMailSender() {
        // When
        emailService.sendSimpleMessage("test@example.com", "Subject", "Body");

        // Then
        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}
