package com.smartspend.copilot.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request DTO for user registration")
public class RegisterRequest {
    @Schema(
            description = "Username for the new account (3-20 characters)",
            example = "john_doe"
    )
    @NotBlank(message = "USERNAME_BLANK")
    @Size(min = 3, max = 20, message = "USERNAME_INVALID_LENGTH")
    String username;

    @Schema(
            description = "Email address for the new account",
            example = "john.doe@example.com"
    )
    @NotBlank(message = "EMAIL_BLANK")
    @Email(message = "INVALID_EMAIL")
    String email;

    @Schema(
            description = "Password for the new account (minimum 8 characters, including uppercase, lowercase, digit, and special character)",
            example = "SecurePass123!"
    )
    @NotBlank(message = "PASSWORD_BLANK")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "PASSWORD_INVALID"
    )
    String password;
}
