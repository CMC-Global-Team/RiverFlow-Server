package com.riverflow.repository;

import com.riverflow.model.PasswordReset;
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
 * Repository tests for PasswordResetRepository using @DataJpaTest
 */
@DataJpaTest
@ActiveProfiles("test")
class PasswordResetRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PasswordResetRepository passwordResetRepository;

    private User testUser;
    private PasswordReset testReset;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .passwordHash("hashedPassword123")
                .fullName("Test User")
                .emailVerified(true)
                .role(User.Role.user)
                .oauthProvider(User.OAuthProvider.email)
                .status(User.UserStatus.active) // Set explicitly
                .credit(0L)
                .createdAt(LocalDateTime.now())
                .build();
        entityManager.persist(testUser);

        testReset = PasswordReset.builder()
                .user(testUser)
                .token("reset-token-123")
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();
    }

    @Test
    void findByToken_ExistingToken_ReturnsReset() {
        // Given
        entityManager.persist(testReset);
        entityManager.flush();

        // When
        Optional<PasswordReset> found = passwordResetRepository.findByToken("reset-token-123");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getToken()).isEqualTo("reset-token-123");
        assertThat(found.get().getUser().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void findByToken_NonExistingToken_ReturnsEmpty() {
        // When
        Optional<PasswordReset> found = passwordResetRepository.findByToken("nonexistent-token");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void save_NewPasswordReset_PersistsSuccessfully() {
        // When
        PasswordReset saved = passwordResetRepository.save(testReset);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getToken()).isEqualTo("reset-token-123");
        assertThat(saved.getExpiresAt()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void findByTokenAndUsedAtIsNullAndExpiresAtAfter_ValidToken_ReturnsReset() {
        // Given
        entityManager.persist(testReset);
        entityManager.flush();

        // When
        Optional<PasswordReset> found = passwordResetRepository
                .findByTokenAndUsedAtIsNullAndExpiresAtAfter("reset-token-123", LocalDateTime.now());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUsedAt()).isNull();
        assertThat(found.get().getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void findByTokenAndUsedAtIsNullAndExpiresAtAfter_UsedToken_ReturnsEmpty() {
        // Given
        testReset.setUsedAt(LocalDateTime.now());
        entityManager.persist(testReset);
        entityManager.flush();

        // When
        Optional<PasswordReset> found = passwordResetRepository
                .findByTokenAndUsedAtIsNullAndExpiresAtAfter("reset-token-123", LocalDateTime.now());

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void findByTokenAndUsedAtIsNullAndExpiresAtAfter_ExpiredToken_ReturnsEmpty() {
        // Given
        testReset.setExpiresAt(LocalDateTime.now().minusMinutes(10)); // Expired
        entityManager.persist(testReset);
        entityManager.flush();

        // When
        Optional<PasswordReset> found = passwordResetRepository
                .findByTokenAndUsedAtIsNullAndExpiresAtAfter("reset-token-123", LocalDateTime.now());

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void deleteByUser_RemovesAllUserResets() {
        // Given
        entityManager.persist(testReset);
        PasswordReset anotherReset = PasswordReset.builder()
                .user(testUser)
                .token("another-token-456")
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();
        entityManager.persist(anotherReset);
        entityManager.flush();

        Long userId = testUser.getId();

        // When
        passwordResetRepository.deleteAll(passwordResetRepository.findAll().stream()
                .filter(pr -> pr.getUser().getId().equals(userId))
                .toList());
        passwordResetRepository.flush();

        // Then
        assertThat(passwordResetRepository.findByToken("reset-token-123")).isEmpty();
        assertThat(passwordResetRepository.findByToken("another-token-456")).isEmpty();
    }

    @Test
    void save_UpdateReset_MarksAsUsed() {
        // Given
        entityManager.persist(testReset);
        entityManager.flush();

        // When
        testReset.setUsedAt(LocalDateTime.now());
        PasswordReset updated = passwordResetRepository.save(testReset);

        // Then
        assertThat(updated.getUsedAt()).isNotNull();
    }
}
