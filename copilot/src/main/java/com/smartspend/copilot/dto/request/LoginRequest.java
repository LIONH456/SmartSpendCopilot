package com.smartspend.copilot.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request DTO for user login")
public class LoginRequest {
    @Schema(
            description = "Username or email address for authentication",
            example = "john_doe"
    )
    @NotBlank(message = "VALIDATION_ERROR")
    String login;

    @Schema(
            description = "User password",
            example = "SecurePass123!"
    )
    @NotBlank(message = "VALIDATION_ERROR")
    String password;
}
