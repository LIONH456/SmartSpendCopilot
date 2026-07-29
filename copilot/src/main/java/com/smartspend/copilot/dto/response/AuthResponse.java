package com.smartspend.copilot.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@Schema(description = "Response DTO for successful login, contains JWT token and username")
public class AuthResponse {
    @Schema(
            description = "JWT authentication token to be used in subsequent requests",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    )
    private String token;

    @Schema(
            description = "Authenticated username",
            example = "johndoe"
    )
    private String username;
}
