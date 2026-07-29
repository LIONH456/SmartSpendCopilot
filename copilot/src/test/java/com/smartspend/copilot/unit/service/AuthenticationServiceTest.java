package com.smartspend.copilot.unit.service;

import com.smartspend.copilot.dto.request.LoginRequest;
import com.smartspend.copilot.dto.request.RegisterRequest;
import com.smartspend.copilot.entity.User;
import com.smartspend.copilot.exception.AppException;
import com.smartspend.copilot.exception.ErrorCode;
import com.smartspend.copilot.repository.UserRepository;
import com.smartspend.copilot.service.AuthenticationService;
import com.smartspend.copilot.service.JwtService;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthenticationServiceTest {
    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    JwtService jwtService;

    @InjectMocks
    AuthenticationService authenticationService;

    User user;
    RegisterRequest registerRequest;
    LoginRequest loginRequest;

    @BeforeEach
    void setUp(){
        user  = new User();
        user.setEmail("fake@gmail.com");
        user.setUsername("fakeUser");
        user.setPassword("fakePassword");

        loginRequest = new LoginRequest();
        loginRequest.setLogin("fakeUser");
        loginRequest.setPassword("fakePassword");

        registerRequest = new RegisterRequest();
        registerRequest.setUsername("jh168");
        registerRequest.setPassword("123");
        registerRequest.setEmail("jh168@gmail.com");
    }

    @Test
    void shouldRegisterSuccessfully(){
        // Arrange
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");

        // Act
        authenticationService.register(registerRequest);

        // Assert and Verify
        verify(passwordEncoder).encode(registerRequest.getPassword());
        // 声明一个专门用来捕获 User 类型对象的捕获器（口袋）
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        // verify(userRepository).save(...) 检查在 Act（执行）阶段，userRepository.save() 方法是否被调用过。
        // captor.capture() 扮演一个“卧底”。当 save 方法被调用时，它会抓取当时传入的那个 User 实例并保存到内部。
        verify(userRepository).save(captor.capture());

        User savedUser = captor.getValue();
        assertEquals(registerRequest.getUsername(), savedUser.getUsername() );
        assertEquals(registerRequest.getEmail(), savedUser.getEmail());
        assertEquals("encodedPassword", savedUser.getPassword());
    }

    @Test
    void shouldThrowExceptionWhenUsernameAlreadyExists(){
        // Arrange
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(true);

        // Act
        AppException exception = assertThrows(AppException.class,
                () -> authenticationService.register(registerRequest));

        // Assert
        assertEquals(ErrorCode.USERNAME_ALREADY_EXISTS.getMessage(), exception.getMessage());

        // Verify
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldThrowExceptionWhenEmailAlreadyExists(){
        // Arrange
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        // Act
        AppException exception = assertThrows(AppException.class,
                () -> authenticationService.register(registerRequest));

        // Assert
        assertEquals(ErrorCode.EMAIL_ALREADY_EXISTS.getMessage(), exception.getMessage());

        // Verify
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldLoginSuccessfullyWithUsername(){
        // Arrange
        when(userRepository.findByUsername(loginRequest.getLogin())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())).thenReturn(true);
        when(jwtService.generateToken(user.getUsername())).thenReturn("fakeToken");

        // Act
        var authResponse = authenticationService.login(loginRequest);

        // Assert
        assertEquals("fakeToken", authResponse.getToken());
        assertEquals(user.getUsername(), authResponse.getUsername());

        // Verify
        verify(jwtService).generateToken(user.getUsername());
    }

    @Test
    void shouldLoginSuccessfullyWithEmail(){
        // Arrange
        loginRequest.setLogin("fake@gmail.com");
        when(userRepository.findByUsername(loginRequest.getLogin())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(loginRequest.getLogin())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())).thenReturn(true);
        when(jwtService.generateToken(user.getUsername())).thenReturn("fakeToken");

        // Act
        var authResponse = authenticationService.login(loginRequest);

        // Assert
        assertEquals("fakeToken", authResponse.getToken());
        assertEquals(user.getUsername(), authResponse.getUsername());

        // Verify
        verify(jwtService).generateToken(user.getUsername());
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound(){
        // Arrange
        when(userRepository.findByUsername(loginRequest.getLogin())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(loginRequest.getLogin())).thenReturn(Optional.empty());

        // Act
        AppException exception = assertThrows(AppException.class,
                () -> authenticationService.login(loginRequest));

        // Assert
        assertEquals(ErrorCode.USER_NOT_FOUND.getMessage(), exception.getMessage());

        // Verify
        verify(jwtService, never()).generateToken(anyString());
    }

    @Test
    void shouldThrowExceptionWhenPasswordIsInvalid(){
        // Arrange
        when(userRepository.findByUsername(loginRequest.getLogin())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())).thenReturn(false);

        // Act
        AppException exception = assertThrows(AppException.class,
                () -> authenticationService.login(loginRequest));

        // Assert
        assertEquals(ErrorCode.INVALID_CREDENTIALS.getMessage(), exception.getMessage());

        // Verify
        verify(jwtService, never()).generateToken(anyString());
    }
}

