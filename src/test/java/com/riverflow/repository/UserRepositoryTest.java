package com.riverflow.repository;

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
 * Repository tests for UserRepository using @DataJpaTest
 * Tests run with H2 in-memory database
 */
@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .passwordHash("hashedPassword123")
                .fullName("Test User")
                .emailVerified(true)
                .role(User.Role.user)
                .oauthProvider(User.OAuthProvider.email)
                .status(User.UserStatus.active)
                .credit(0L)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void findByEmail_ExistingUser_ReturnsUser() {
        // Given
        entityManager.persist(testUser);
        entityManager.flush();

        // When
        Optional<User> found = userRepository.findByEmail("test@example.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
        assertThat(found.get().getFullName()).isEqualTo("Test User");
    }

    @Test
    void findByEmail_NonExistingUser_ReturnsEmpty() {
        // When
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void existsByEmail_ExistingUser_ReturnsTrue() {
        // Given
        entityManager.persist(testUser);
        entityManager.flush();

        // When
        Boolean exists = userRepository.existsByEmail("test@example.com");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void existsByEmail_NonExistingUser_ReturnsFalse() {
        // When
        Boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void save_NewUser_PersistsSuccessfully() {
        // Given
        User newUser = User.builder()
                .email("new@example.com")
                .passwordHash("hashedPassword")
                .fullName("New User")
                .emailVerified(false)
                .role(User.Role.user)
                .oauthProvider(User.OAuthProvider.email)
                .status(User.UserStatus.active) // Set explicitly
                .build();

        // When
        User savedUser = userRepository.save(newUser);

        // Then
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getEmail()).isEqualTo("new@example.com");
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isNotNull();
    }

    @Test
    void save_UpdateExistingUser_UpdatesSuccessfully() {
        // Given
        User existingUser = User.builder()
                .email("update@example.com")
                .passwordHash("oldHash")
                .fullName("Old Name")
                .emailVerified(true)
                .role(User.Role.user)
                .oauthProvider(User.OAuthProvider.email)
                .status(User.UserStatus.active) // Set explicitly
                .build();
        entityManager.persist(existingUser);
        entityManager.flush();

        // When
        existingUser.setFullName("Updated Name");
        User updatedUser = userRepository.save(existingUser);

        // Then
        assertThat(updatedUser.getFullName()).isEqualTo("Updated Name");
    }

    @Test
    void findById_ExistingUser_ReturnsUser() {
        // Given
        User user = User.builder()
                .email("findbyid@example.com")
                .passwordHash("hash")
                .fullName("Find Me")
                .emailVerified(true)
                .role(User.Role.user)
                .oauthProvider(User.OAuthProvider.email)
                .status(User.UserStatus.active) // Set explicitly
                .build();
        User persistedUser = entityManager.persist(user);
        entityManager.flush();

        // When
        Optional<User> foundUser = userRepository.findById(persistedUser.getId());

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("findbyid@example.com");
    }

    @Test
    void delete_ExistingUser_RemovesFromDatabase() {
        // Given
        entityManager.persist(testUser);
        entityManager.flush();
        Long userId = testUser.getId();

        // When
        userRepository.deleteById(userId);

        // Then
        Optional<User> found = userRepository.findById(userId);
        assertThat(found).isEmpty();
    }
}
