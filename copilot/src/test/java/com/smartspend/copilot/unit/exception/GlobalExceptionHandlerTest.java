package com.smartspend.copilot.unit.exception;

import com.smartspend.copilot.controller.AuthenticationController;
import com.smartspend.copilot.dto.request.RegisterRequest;
import com.smartspend.copilot.exception.AppException;
import com.smartspend.copilot.exception.ErrorCode;
import com.smartspend.copilot.exception.GlobalExceptionHandler;
import com.smartspend.copilot.service.AuthenticationService;
import com.smartspend.copilot.service.CustomUserDetailService;
import com.smartspend.copilot.service.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthenticationController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
public class GlobalExceptionHandlerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailService customUserDetailService;

    @Test
    void shouldHandleAppException() throws Exception {
        // Arrange
        doThrow(new AppException(ErrorCode.USERNAME_ALREADY_EXISTS))
                .when(authenticationService).register(any(RegisterRequest.class));

        RegisterRequest request = new RegisterRequest();
        request.setUsername("jh168");
        request.setEmail("jh168@gmail.com");
        request.setPassword("SecurePass123!");
        String jsonRequest = objectMapper.writeValueAsString(request);

        // Act and Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.USERNAME_ALREADY_EXISTS.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.USERNAME_ALREADY_EXISTS.getMessage()))
                .andExpect(jsonPath("$.status").value(ErrorCode.USERNAME_ALREADY_EXISTS.getStatus().value()))
                .andExpect(jsonPath("$.path").value("/api/auth/register"));
    }

    @Test
    void shouldHandleValidationException() throws Exception {
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
                .andExpect(jsonPath("$.code").value(ErrorCode.USERNAME_BLANK.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.USERNAME_BLANK.getMessage()));
    }

    @Test
    void shouldHandleGenericException() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Something went wrong"))
                .when(authenticationService).register(any(RegisterRequest.class));

        RegisterRequest request = new RegisterRequest();
        request.setUsername("jh168");
        request.setEmail("jh168@gmail.com");
        request.setPassword("SecurePass123!");
        String jsonRequest = objectMapper.writeValueAsString(request);

        // Act and Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(ErrorCode.INTERNAL_SERVER_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value("Something went wrong"));
    }
}
