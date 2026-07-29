package com.smartspend.copilot.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request DTO for resetting the authenticated user's password")
public class ResetPasswordRequest {
    @Schema(description = "Current password for verification", example = "CurrentPass123!")
    @NotBlank(message = "PASSWORD_BLANK")
    String oldPassword;

    @Schema(description = "New password to set", example = "NewPass123!")
    @NotBlank(message = "PASSWORD_BLANK")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "PASSWORD_INVALID"
    )
    String newPassword;
}
