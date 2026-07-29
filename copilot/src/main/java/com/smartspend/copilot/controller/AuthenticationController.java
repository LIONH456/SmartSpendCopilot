package com.smartspend.copilot.controller;

import com.smartspend.copilot.dto.request.LoginRequest;
import com.smartspend.copilot.dto.request.RegisterRequest;
import com.smartspend.copilot.dto.request.ResetPasswordRequest;
import com.smartspend.copilot.dto.response.ApiErrorResponse;
import com.smartspend.copilot.dto.response.AuthResponse;
import com.smartspend.copilot.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication API", description = "APIs for user registration, login, and JWT token management")
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    @Operation(
            summary = "User registration",
            description = "Register a new user account with username, email, and password. Requires password to meet complexity requirements."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request, validation failed, username or email already exists",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
    })
    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request){
        authenticationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(
            summary = "User login",
            description = "Authenticate user with username/email and password, returns JWT token for authenticated requests"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful, returns JWT token"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials or user not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request){
        AuthResponse response = authenticationService.login(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Reset current user's password",
            description = "Verifies the current password before updating it for the authenticated user"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password reset successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or password validation failed",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Current password is incorrect",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PutMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authenticationService.resetPassword(request);
        return ResponseEntity.ok().build();
    }
}
