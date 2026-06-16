package com.smartspend.copilot.integration.repository;

import com.smartspend.copilot.entity.User;
import com.smartspend.copilot.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
public class UserRepositoryIntegrationTest {
    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindUserByUsername() {
        // Arrange
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .build();

        // Act
        User savedUser = userRepository.save(user);
        User foundUser = userRepository.findByUsername("testuser").orElse(null);

        // Assert
        assertNotNull(savedUser);
        assertNotNull(savedUser.getId());
        assertNotNull(foundUser);
        assertEquals(savedUser.getUsername(), "testuser");
    }

    @Test
    void shouldSaveAndFindUserByEmail() {
        // Arrange
        User user = User.builder()
                .username("testuser2")
                .email("test2@example.com")
                .password("encodedPassword")
                .build();

        // Act
        userRepository.save(user);
        User foundUser = userRepository.findByEmail("test2@example.com").orElse(null);

        // Assert
        assertNotNull(foundUser);
        assertEquals(foundUser.getEmail(), "test2@example.com");
    }

    @Test
    void shouldCheckIfUserExistsByUsername() {
        // Arrange
        User user = User.builder()
                .username("existinguser")
                .email("existing@example.com")
                .password("encodedPassword")
                .build();

        // Act
        userRepository.save(user);
        boolean exists = userRepository.existsByUsername("existinguser");
        boolean notExists = userRepository.existsByUsername("nonexistent");

        // Assert
        assertTrue(exists);
        assertFalse(notExists);
    }

    @Test
    void shouldCheckIfUserExistsByEmail() {
        // Arrange
        User user = User.builder()
                .username("emailuser")
                .email("email@example.com")
                .password("encodedPassword")
                .build();

        // Act
        userRepository.save(user);
        boolean exists = userRepository.existsByEmail("email@example.com");
        boolean notExists = userRepository.existsByEmail("nonexistent@example.com");

        // Assert
        assertTrue(exists);
        assertFalse(notExists);
    }
}
