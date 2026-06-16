package com.smartspend.copilot.unit.service;


import com.smartspend.copilot.exception.AppException;
import com.smartspend.copilot.exception.ErrorCode;
import com.smartspend.copilot.service.JwtService;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class JwtServiceTest {
    JwtService jwtService;
    String SECRET = "mySuperSecretKeyForJWTTesting012345678901234567890";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 86400000L);
    }

    @Test
    void shouldGeneratedTokenSuccessfully(){
        String token = jwtService.generateToken("lionh456");
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void shouldExtractUsernameFromToken(){
        String token = jwtService.generateToken("lionh456");
        String username = jwtService.extractUsername(token);
        assertEquals("lionh456", username);
    }

    @Test
    void shouldReturnTrueWhenTokenMatchesUser(){
        String token = jwtService.generateToken("lionh456");
        UserDetails userDetails = new User("lionh456", "123password", List.of());
        boolean valid = jwtService.isTokenValid(token, userDetails);
        assertTrue(valid);
    }

    @Test
    void shouldReturnFalseWhenUsernameDoesNotMatch() {
        String token = jwtService.generateToken("lionh456");
        UserDetails userDetails = new User("jh168", "12345678", List.of());
        boolean valid = jwtService.isTokenValid(token, userDetails);
        assertFalse(valid);
    }

    @Test
    void shouldThrowAppExceptionWhenTokenIsExpired(){
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1000L);
        String token = jwtService.generateToken("lionh456");
        UserDetails userDetails = new User("lionh456", "12345678", List.of());
        
        AppException exception = assertThrows(AppException.class,
                ()-> jwtService.isTokenValid(token, userDetails));
        
        assertEquals(ErrorCode.TOKEN_EXPIRED, exception.getErrorCode());
    }

    @Test
    void shouldThrowAppExceptionForTamperedToken(){
        String token = jwtService.generateToken("lionh456");
        String tamperedToken = token + "abc";
        
        AppException exception = assertThrows(AppException.class,
                () -> jwtService.extractUsername(tamperedToken));
        
        assertEquals(ErrorCode.INVALID_TOKEN, exception.getErrorCode());
    }
}
