package com.smartspend.copilot.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Collections;
import java.util.Map;

// 监听全项目 controller exception
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity <Map<String, Object>> handleTransactionNotFoundException
            (TransactionNotFoundException e){
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "error", e.getMessage(),
                        "timestamp", System.currentTimeMillis()
                ));
    }

    @ExceptionHandler(AIParsingException.class)
    public ResponseEntity<Map<String, Object>> handleAIParsingException
            (AIParsingException e){
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", e.getMessage(),
                        "timestamp", System.currentTimeMillis()
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException e){
        return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage(),
                "timestamp", System.currentTimeMillis()
        ));
    }
}
