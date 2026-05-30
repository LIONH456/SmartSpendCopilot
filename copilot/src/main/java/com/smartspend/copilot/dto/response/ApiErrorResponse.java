package com.smartspend.copilot.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class ApiErrorResponse {
    @Schema(description = "Local timestamp of the error", example = "2026-05-29T09:36:34.651Z")
    LocalDateTime timestamp; // 错误发生时间。
    @Schema(description = "HTTP status code", example = "400")
    int status; // HTTP Status Code
    @Schema(description = "The unique internal business error code identifying the specific failure reason", example = "1001")
    int code;
    @Schema(description = "HTTP Status name", example = "Bad Request")
    String error; // HTTP 状态名字：Bad Request, Not Found, Internal Server Error
    @Schema(description = "Specific reasons for business errors", example = "Description cannot be blank")
    String message; // 真正业务错误
    @Schema(description = "API request path that triggered the crash", example = "/api/transactions/process")
    String path; // 哪个 endpoint 爆炸
}
