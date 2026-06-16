package com.smartspend.copilot.integration;

import com.smartspend.copilot.dto.request.LoginRequest;
import com.smartspend.copilot.dto.request.RegisterRequest;
import com.smartspend.copilot.entity.User;
import com.smartspend.copilot.exception.ErrorCode;
import com.smartspend.copilot.repository.UserRepository;
import com.smartspend.copilot.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthenticationIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void shouldRegisterSuccessfully() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");
        request.setPassword("SecurePass123!");

        // Act
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Assert
        User savedUser = userRepository.findByUsername("newuser").orElse(null);
        assertNotNull(savedUser);
        assertEquals(savedUser.getEmail(), "new@example.com");
    }

    @Test
    void shouldFailRegisterWithDuplicateUsername() throws Exception {
        // Arrange
        User existingUser = User.builder()
                .username("existinguser")
                .email("existing@example.com")
                .password(passwordEncoder.encode("SecurePass123!"))
                .build();
        userRepository.save(existingUser);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("existinguser");
        request.setEmail("different@example.com");
        request.setPassword("SecurePass123!");

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.USERNAME_ALREADY_EXISTS.getCode()));
    }

    @Test
    void shouldFailRegisterWithDuplicateEmail() throws Exception {
        // Arrange
        User existingUser = User.builder()
                .username("someuser")
                .email("duplicate@example.com")
                .password(passwordEncoder.encode("SecurePass123!"))
                .build();
        userRepository.save(existingUser);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser2");
        request.setEmail("duplicate@example.com");
        request.setPassword("SecurePass123!");

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.EMAIL_ALREADY_EXISTS.getCode()));
    }

    @Test
    void shouldLoginSuccessfullyWithUsername() throws Exception {
        // Arrange
        User user = User.builder()
                .username("loginuser")
                .email("login@example.com")
                .password(passwordEncoder.encode("SecurePass123!"))
                .build();
        userRepository.save(user);

        LoginRequest request = new LoginRequest();
        request.setLogin("loginuser");
        request.setPassword("SecurePass123!");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void shouldLoginSuccessfullyWithEmail() throws Exception {
        // Arrange
        User user = User.builder()
                .username("emailuser")
                .email("useremail@example.com")
                .password(passwordEncoder.encode("SecurePass123!"))
                .build();
        userRepository.save(user);

        LoginRequest request = new LoginRequest();
        request.setLogin("useremail@example.com");
        request.setPassword("SecurePass123!");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void shouldFailLoginWithWrongPassword() throws Exception {
        // Arrange
        User user = User.builder()
                .username("wrongpass")
                .email("wrongpass@example.com")
                .password(passwordEncoder.encode("SecurePass123!"))
                .build();
        userRepository.save(user);

        LoginRequest request = new LoginRequest();
        request.setLogin("wrongpass");
        request.setPassword("WrongPass123!");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_CREDENTIALS.getCode()));
    }

    @Test
    void shouldFailLoginWithNonExistentUser() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setLogin("nonexistent");
        request.setPassword("SecurePass123!");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_CREDENTIALS.getCode()));
    }
}
