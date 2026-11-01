package com.taskmanager.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<Map<String, String>> notFound(NotFoundException ex) { return error(HttpStatus.NOT_FOUND, ex.getMessage()); }
    @ExceptionHandler(BadRequestException.class)
    ResponseEntity<Map<String, String>> badRequest(BadRequestException ex) { return error(HttpStatus.BAD_REQUEST, ex.getMessage()); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> invalid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream().findFirst().map(e -> e.getField() + " " + e.getDefaultMessage()).orElse("Validation failed");
        return error(HttpStatus.BAD_REQUEST, message);
    }
    private ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }
}
