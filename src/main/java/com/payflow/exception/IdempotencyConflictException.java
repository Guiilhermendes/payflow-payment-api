package com.payflow.exception;

public class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException(String message) { super(message); }
}
