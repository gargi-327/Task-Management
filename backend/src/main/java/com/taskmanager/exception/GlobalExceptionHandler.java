package com.taskmanager.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<String> handleBadRequest(BadRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ex.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<String> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<String> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {

        String parameter = ex.getName();

        if ("priority".equalsIgnoreCase(parameter)) {
            return ResponseEntity.badRequest()
                    .body("Invalid priority. Allowed values: LOW, MEDIUM, HIGH");
        }

        if ("status".equalsIgnoreCase(parameter)) {
            return ResponseEntity.badRequest()
                    .body("Invalid status. Allowed values: TODO, IN_PROGRESS, DONE");
        }

        return ResponseEntity.badRequest()
                .body("Invalid value for parameter: " + parameter);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> handleInvalidRequestBody(
            HttpMessageNotReadableException ex) {

        String message = ex.getMessage();

        if (message != null && message.contains("TaskPriority")) {
            return ResponseEntity.badRequest()
                    .body("Invalid priority. Allowed values: LOW, MEDIUM, HIGH");
        }

        if (message != null && message.contains("TaskStatus")) {
            return ResponseEntity.badRequest()
                    .body("Invalid status. Allowed values: TODO, IN_PROGRESS, DONE");
        }

        return ResponseEntity.badRequest()
                .body("Invalid request body or enum value");
    }
}