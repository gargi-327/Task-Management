package com.taskmanager.exception;

import com.taskmanager.exception.ConflictException;
import com.taskmanager.exception.ForbiddenException;
import com.taskmanager.exception.UnauthorizedException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<Map<String, String>> error(
            HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(Map.of("message", message));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, String>> handleUnauthorized(
            UnauthorizedException ex) {
        return error(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, String>> handleForbidden(
            ForbiddenException ex) {
        return error(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, String>> handleConflict(
            ConflictException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(BadRequestException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex) {
        if (isKnownUniqueConstraintViolation(ex)) {
            return error(HttpStatus.CONFLICT, "A record with the supplied unique value already exists");
        }
        return error(HttpStatus.BAD_REQUEST, "Request violates a data integrity constraint");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(
            MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult().getFieldErrors()
                .stream()
                .findFirst()
                .map(e -> e.getField() + " " + e.getDefaultMessage())
                .orElse("Validation failed");

        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {

        String parameter = ex.getName();

        if ("priority".equalsIgnoreCase(parameter)) {
            return error(HttpStatus.BAD_REQUEST,
                    "Invalid priority. Allowed values: LOW, MEDIUM, HIGH");
        }

        if ("status".equalsIgnoreCase(parameter)) {
            return error(HttpStatus.BAD_REQUEST,
                    "Invalid status. Allowed values: TODO, IN_PROGRESS, DONE");
        }

        return error(HttpStatus.BAD_REQUEST,
                "Invalid value for parameter: " + parameter);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleInvalidRequestBody(
            HttpMessageNotReadableException ex) {

        String message = ex.getMessage();

        if (message != null && message.contains("TaskPriority")) {
            return error(HttpStatus.BAD_REQUEST,
                    "Invalid priority. Allowed values: LOW, MEDIUM, HIGH");
        }

        if (message != null && message.contains("TaskStatus")) {
            return error(HttpStatus.BAD_REQUEST,
                    "Invalid status. Allowed values: TODO, IN_PROGRESS, DONE");
        }

        return error(HttpStatus.BAD_REQUEST,
                "Invalid request body or enum value");
    }

    private boolean isKnownUniqueConstraintViolation(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof DuplicateKeyException) {
                return true;
            }
            if (cause instanceof ConstraintViolationException violation) {
                String constraintName = violation.getConstraintName();
                return "uk_users_email".equalsIgnoreCase(constraintName)
                        || "uk_project_member".equalsIgnoreCase(constraintName);
            }
        }
        return false;
    }
}