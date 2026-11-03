package com.payflow.exception;

import com.payflow.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(PaymentNotFoundException.class)
    ResponseEntity<ErrorResponse> notFound(PaymentNotFoundException e, HttpServletRequest r) {
        return error(HttpStatus.NOT_FOUND, e.getMessage(), r);
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    ResponseEntity<ErrorResponse> conflict(IdempotencyConflictException e, HttpServletRequest r) {
        return error(HttpStatus.CONFLICT, e.getMessage(), r);
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    ResponseEntity<ErrorResponse> badRequest(Exception e, HttpServletRequest r) {
        String message = e instanceof MethodArgumentNotValidException validation
                ? validation.getBindingResult().getFieldErrors().stream().findFirst().map(error -> error.getField() + ": " + error.getDefaultMessage()).orElse("Invalid request")
                : e.getMessage();
        return error(HttpStatus.BAD_REQUEST, message, r);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception e, HttpServletRequest r) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", r);
    }
    
    private ResponseEntity<ErrorResponse> error(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ErrorResponse(Instant.now(), status.value(),
                status.getReasonPhrase(), message, request.getRequestURI()));
    }
}
