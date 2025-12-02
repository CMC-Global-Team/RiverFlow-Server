package com.riverflow.repository;

import com.riverflow.model.EmailVerification;
import com.riverflow.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository tests for EmailVerificationRepository using @DataJpaTest
 */
@DataJpaTest
@ActiveProfiles("test")
class EmailVerificationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    private User testUser;
    private EmailVerification testVerification;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .passwordHash("hashedPassword123")
                .fullName("Test User")
                .emailVerified(false)
                .role(User.Role.user)
                .oauthProvider(User.OAuthProvider.email)
                .status(User.UserStatus.active) // Set explicitly
                .credit(0L)
                .createdAt(LocalDateTime.now())
                .build();
        entityManager.persist(testUser);

        testVerification = EmailVerification.builder()
                .user(testUser)
                .token("test-token-123")
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
    }

    @Test
    void findByToken_ExistingToken_ReturnsVerification() {
        // Given
        entityManager.persist(testVerification);
        entityManager.flush();

        // When
        Optional<EmailVerification> found = emailVerificationRepository.findByToken("test-token-123");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getToken()).isEqualTo("test-token-123");
        assertThat(found.get().getUser().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void findByToken_NonExistingToken_ReturnsEmpty() {
        // When
        Optional<EmailVerification> found = emailVerificationRepository.findByToken("nonexistent-token");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void save_NewVerification_PersistsSuccessfully() {
        // When
        EmailVerification saved = emailVerificationRepository.save(testVerification);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getToken()).isEqualTo("test-token-123");
        assertThat(saved.getExpiresAt()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void deleteAllByUser_RemovesAllUserVerifications() {
        // Given
        entityManager.persist(testVerification);
        EmailVerification anotherVerification = EmailVerification.builder()
                .user(testUser)
                .token("another-token-456")
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
        entityManager.persist(anotherVerification);
        entityManager.flush();

        // When
        emailVerificationRepository.deleteAllByUser(testUser);
        emailVerificationRepository.flush();

        // Then
        Optional<EmailVerification> found1 = emailVerificationRepository.findByToken("test-token-123");
        Optional<EmailVerification> found2 = emailVerificationRepository.findByToken("another-token-456");
        assertThat(found1).isEmpty();
        assertThat(found2).isEmpty();
    }

    @Test
    void save_UpdateVerification_MarksAsVerified() {
        // Given
        entityManager.persist(testVerification);
        entityManager.flush();

        // When
        testVerification.setVerifiedAt(LocalDateTime.now());
        EmailVerification updated = emailVerificationRepository.save(testVerification);

        // Then
        assertThat(updated.getVerifiedAt()).isNotNull();
    }
}
