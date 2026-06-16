package com.smartspend.copilot.unit.controller;

import com.smartspend.copilot.controller.AuthenticationController;
import com.smartspend.copilot.dto.request.LoginRequest;
import com.smartspend.copilot.dto.request.RegisterRequest;
import com.smartspend.copilot.exception.AppException;
import com.smartspend.copilot.exception.ErrorCode;
import com.smartspend.copilot.service.AuthenticationService;
import com.smartspend.copilot.service.CustomUserDetailService;
import com.smartspend.copilot.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.mockito.Mockito.*;

@WebMvcTest(AuthenticationController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthenticationControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    AuthenticationService authenticationService;

    @MockitoBean
    JwtService jwtService;

    @MockitoBean
    private CustomUserDetailService customUserDetailService;

    RegisterRequest registerRequest;
    LoginRequest loginRequest;

    @BeforeEach
    void setUp(){
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("jh168");
        registerRequest.setEmail("jh168@gmail.com");
        registerRequest.setPassword("SecurePass123!");

        loginRequest = new LoginRequest();
        loginRequest.setLogin("jh168");
        loginRequest.setPassword("SecurePass123!");
    }

    @Test
    void shouldRegisterSuccessfully() throws Exception {
        // Arrange
        String jsonRequest = objectMapper.writeValueAsString(registerRequest);

        // Act and Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
            .andExpect(status().isCreated());

        // Verify
        verify(authenticationService).register(any(RegisterRequest.class));
    }

    @Test
    void shouldThrowExceptionWhenUsernameExists() throws Exception {
        // Arrange
        doThrow(new AppException(ErrorCode.USERNAME_ALREADY_EXISTS))
                .when(authenticationService).register(any(RegisterRequest.class));
        String jsonRequest = objectMapper.writeValueAsString(registerRequest);

        // Act and Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ErrorCode.USERNAME_ALREADY_EXISTS.getMessage()))
                .andExpect(jsonPath("$.code").value(ErrorCode.USERNAME_ALREADY_EXISTS.getCode()));

        // Verify
        verify(authenticationService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    void shouldThrowExceptionWhenEmailExists() throws Exception {
        // Arrange
        doThrow(new AppException(ErrorCode.EMAIL_ALREADY_EXISTS))
                .when(authenticationService).register(any(RegisterRequest.class));
        String jsonRequest = objectMapper.writeValueAsString(registerRequest);

        // Act and Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ErrorCode.EMAIL_ALREADY_EXISTS.getMessage()))
                .andExpect(jsonPath("$.code").value(ErrorCode.EMAIL_ALREADY_EXISTS.getCode()));

        // Verify
        verify(authenticationService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    void shouldLoginSuccessfully() throws Exception {
        // Arrange
        String fakeToken = UUID.randomUUID().toString();
        when(authenticationService.login(any(LoginRequest.class))).thenReturn(fakeToken);
        String jsonRequest = objectMapper.writeValueAsString(loginRequest);

        // Act and Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(fakeToken));

        // Verify
        verify(authenticationService).login(any(LoginRequest.class));
    }

    @Test
    void shouldThrowExceptionWhenCredentialInvalid() throws Exception {
        // Arrange
        String jsonRequest = objectMapper.writeValueAsString(loginRequest);
        when(authenticationService.login(any(LoginRequest.class)))
                .thenThrow(new AppException(ErrorCode.INVALID_CREDENTIALS));

        // Act and Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_CREDENTIALS.getMessage()))
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_CREDENTIALS.getCode()));

        // Verify
        verify(authenticationService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    void shouldThrowValidationErrorWhenUsernameBlank() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("");
        request.setEmail("jh168@gmail.com");
        request.setPassword("SecurePass123!");
        String jsonRequest = objectMapper.writeValueAsString(request);

        // Act and Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.USERNAME_BLANK.getCode()));
    }

    @Test
    void shouldThrowValidationErrorWhenUsernameTooShort() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("ab");
        request.setEmail("jh168@gmail.com");
        request.setPassword("SecurePass123!");
        String jsonRequest = objectMapper.writeValueAsString(request);

        // Act and Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.USERNAME_INVALID_LENGTH.getCode()));
    }

    @Test
    void shouldThrowValidationErrorWhenUsernameTooLong() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("a".repeat(21));
        request.setEmail("jh168@gmail.com");
        request.setPassword("SecurePass123!");
        String jsonRequest = objectMapper.writeValueAsString(request);

        // Act and Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.USERNAME_INVALID_LENGTH.getCode()));
    }

    @Test
    void shouldThrowValidationErrorWhenEmailBlank() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("jh168");
        request.setEmail("");
        request.setPassword("SecurePass123!");
        String jsonRequest = objectMapper.writeValueAsString(request);

        // Act and Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.EMAIL_BLANK.getCode()));
    }

    @Test
    void shouldThrowValidationErrorWhenEmailInvalid() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("jh168");
        request.setEmail("invalid-email");
        request.setPassword("SecurePass123!");
        String jsonRequest = objectMapper.writeValueAsString(request);

        // Act and Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_EMAIL.getCode()));
    }

    @Test
    void shouldThrowValidationErrorWhenPasswordBlank() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("jh168");
        request.setEmail("jh168@gmail.com");
        request.setPassword("");
        String jsonRequest = objectMapper.writeValueAsString(request);

        // Act and Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.PASSWORD_BLANK.getCode()));
    }

    @Test
    void shouldThrowValidationErrorWhenPasswordTooShort() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("jh168");
        request.setEmail("jh168@gmail.com");
        request.setPassword("Pass1!");
        String jsonRequest = objectMapper.writeValueAsString(request);

        // Act and Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.PASSWORD_INVALID.getCode()));
    }

    @Test
    void shouldThrowValidationErrorWhenPasswordNoUppercase() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("jh168");
        request.setEmail("jh168@gmail.com");
        request.setPassword("securepass123!");
        String jsonRequest = objectMapper.writeValueAsString(request);

        // Act and Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.PASSWORD_INVALID.getCode()));
    }

    @Test
    void shouldThrowValidationErrorWhenPasswordNoLowercase() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("jh168");
        request.setEmail("jh168@gmail.com");
        request.setPassword("SECUREPASS123!");
        String jsonRequest = objectMapper.writeValueAsString(request);

        // Act and Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.PASSWORD_INVALID.getCode()));
    }

    @Test
    void shouldThrowValidationErrorWhenPasswordNoDigit() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("jh168");
        request.setEmail("jh168@gmail.com");
        request.setPassword("SecurePass!");
        String jsonRequest = objectMapper.writeValueAsString(request);

        // Act and Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.PASSWORD_INVALID.getCode()));
    }

    @Test
    void shouldThrowValidationErrorWhenPasswordNoSpecialChar() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("jh168");
        request.setEmail("jh168@gmail.com");
        request.setPassword("SecurePass123");
        String jsonRequest = objectMapper.writeValueAsString(request);

        // Act and Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.PASSWORD_INVALID.getCode()));
    }
}
