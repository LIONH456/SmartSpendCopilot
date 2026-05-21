package com.smartspend.copilot.exception;

// RuntimeException : 这是运行时错误
public class TransactionNotFoundException extends RuntimeException {
    public TransactionNotFoundException(String message) {
        // 把 message 交给 RuntimeException 保存。以后才可以叫e.getMessage()
        super(message);
    }
}
