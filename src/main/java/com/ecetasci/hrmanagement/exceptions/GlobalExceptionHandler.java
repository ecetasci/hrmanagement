package com.ecetasci.hrmanagement.exceptions;

import com.ecetasci.hrmanagement.dto.response.BaseResponse;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Map<String, Object>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
        Map<String, String> errors = fieldErrors.stream()
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (a, b) -> a + ", " + b));

        Map<String, Object> payload = new HashMap<>();
        payload.put("errors", errors);
        payload.put("message", "Validation failed");

        BaseResponse<Map<String, Object>> body = BaseResponse.<Map<String, Object>>builder()
                .success(false)
                .code(400)
                .message("Validation error")
                .data(payload)
                .build();

        return new ResponseEntity<>(body, new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<BaseResponse<Map<String, Object>>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> violations = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        v -> {
                            String path = "";
                            for (ConstraintViolation<?> cv = v; cv != null; ) {
                                path = v.getPropertyPath().toString();
                                break;
                            }
                            return path;
                        },
                        ConstraintViolation::getMessage,
                        (a, b) -> a + ", " + b
                ));

        Map<String, Object> payload = new HashMap<>();
        payload.put("errors", violations);
        payload.put("message", "Constraint violations");

        BaseResponse<Map<String, Object>> body = BaseResponse.<Map<String, Object>>builder()
                .success(false)
                .code(400)
                .message("Constraint violation")
                .data(payload)
                .build();

        return new ResponseEntity<>(body, new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<BaseResponse<Map<String, Object>>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        String msg = "Malformed JSON request";

        Throwable cause = ex.getMostSpecificCause();
        if (cause instanceof InvalidFormatException) {
            InvalidFormatException ife = (InvalidFormatException) cause;
            String targetType = ife.getTargetType() != null ? ife.getTargetType().getSimpleName() : "unknown";
            String value = ife.getValue() != null ? ife.getValue().toString() : "null";
            msg = String.format("Invalid value '%s' for type %s", value, targetType);
        } else if (cause != null) {
            msg = cause.getMessage();
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("error", msg);

        BaseResponse<Map<String, Object>> body = BaseResponse.<Map<String, Object>>builder()
                .success(false)
                .code(400)
                .message("Malformed JSON or type mismatch")
                .data(payload)
                .build();

        return new ResponseEntity<>(body, new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Object>> handleAll(Exception ex) {
        BaseResponse<Object> body = BaseResponse.builder()
                .success(false)
                .code(500)
                .message("Internal server error: " + Objects.toString(ex.getMessage(), ""))
                .data(null)
                .build();
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }}




