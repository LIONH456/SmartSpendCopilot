package com.smartspend.copilot.exception;

public class InvalidTransactionDescriptionException extends AppException {
    public InvalidTransactionDescriptionException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message);
    }
}
