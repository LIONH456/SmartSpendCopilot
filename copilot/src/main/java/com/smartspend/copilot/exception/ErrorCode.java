package com.smartspend.copilot.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ErrorCode {
    USERNAME_ALREADY_EXISTS(1001, "Username already exists", HttpStatus.BAD_REQUEST),
    EMAIL_ALREADY_EXISTS(1002, "Email already exists", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(1003, "User not found", HttpStatus.NOT_FOUND),
    TRANSACTION_NOT_FOUND(2001, "Transaction not found", HttpStatus.NOT_FOUND),
    UNSUPPORTED_CURRENCY_PAIR(3001, "Unsupported currency pair", HttpStatus.BAD_REQUEST),
    EXCHANGE_RATE_UNAVALIABLE(3002, "Exchange rate service unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    AI_PARSING_FAILED(4001, "Failed to parse transaction", HttpStatus.INTERNAL_SERVER_ERROR),
    AI_RESPONSE_INVALID(4002, "AI returned invalid response", HttpStatus.INTERNAL_SERVER_ERROR),
    VALIDATION_ERROR(5001, "Validation failed", HttpStatus.BAD_REQUEST),
    DESCRIPTION_BLANK(5002, "Description cannot be blank", HttpStatus.BAD_REQUEST),
    INTERNAL_SERVER_ERROR(9001, "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

    int code;
    String message;
    HttpStatus status;

}
