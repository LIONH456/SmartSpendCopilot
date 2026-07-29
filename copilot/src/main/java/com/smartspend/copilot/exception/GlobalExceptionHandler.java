package com.smartspend.copilot.exception;

import com.smartspend.copilot.dto.response.ApiErrorResponse;
import com.smartspend.copilot.service.ExchangeRateService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

// 监听全项目 controller exception
@ControllerAdvice
public class GlobalExceptionHandler {

    // HttpServletRequest： 当前的HTTP请求

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiErrorResponse> handleAppException(AppException e, HttpServletRequest request) {
        ErrorCode errorCode = e.getErrorCode();
        Map<String, Object> details = Collections.emptyMap();
        if (e instanceof ClarificationRequiredException clarification) {
            details = Map.of(
                    "item", clarification.getItem(),
                    "val1", clarification.getVal1(),
                    "val2", clarification.getVal2(),
                    "clarification", true
            );
        }

        ApiErrorResponse response = ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(errorCode.getStatus().value())
                .code(errorCode.getCode())
                .error(errorCode.getStatus().getReasonPhrase())
                .message(e.getMessage())
                .details(details)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException
            (MethodArgumentNotValidException e, HttpServletRequest request) {

        var fieldErrors = e.getBindingResult().getFieldErrors();
        var priority = List.of(
                "USERNAME_BLANK",
                "EMAIL_BLANK",
                "PASSWORD_BLANK",
                "USERNAME_INVALID_LENGTH",
                "INVALID_EMAIL",
                "PASSWORD_INVALID"
        );

        String key = "VALIDATION_ERROR";
        int bestIndex = Integer.MAX_VALUE;
        for (var fe : fieldErrors) {
            String message = fe.getDefaultMessage();
            if (message == null || message.isBlank()) {
                continue;
            }
            int index = priority.indexOf(message);
            if (index >= 0 && index < bestIndex) {
                bestIndex = index;
                key = message;
            }
        }
        if ("VALIDATION_ERROR" .equals(key)) {
            key = fieldErrors.stream()
                    .map(fe -> fe.getDefaultMessage())
                    .filter(msg -> msg != null && !msg.isBlank())
                    .findFirst()
                    .orElse("VALIDATION_ERROR");
        }

        ErrorCode errorCode;
        try {
            errorCode = ErrorCode.valueOf(key);
        } catch (IllegalArgumentException ex) {
            errorCode = ErrorCode.VALIDATION_ERROR;
        }

        ApiErrorResponse response = ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(errorCode.getStatus().value())
                .code(errorCode.getCode())
                .error(errorCode.getStatus().getReasonPhrase())
                .message(errorCode.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ApiErrorResponse> handleExpiredToken
            (ExpiredJwtException e, HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.TOKEN_EXPIRED;

        ApiErrorResponse response = ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(errorCode.getStatus().value())
                .code(errorCode.getCode())
                .error(errorCode.getStatus().getReasonPhrase())
                .message(errorCode.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiErrorResponse> handleJwtException
            (JwtException e, HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.INVALID_TOKEN;

        ApiErrorResponse response = ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(errorCode.getStatus().value())
                .code(errorCode.getCode())
                .error(errorCode.getStatus().getReasonPhrase())
                .message(errorCode.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(errorCode.getStatus()).body(response);

    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException
            (Exception e, HttpServletRequest request) {
        ApiErrorResponse response = ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .code(9001)
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message(e.getMessage())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.internalServerError().body(response);
    }
}
