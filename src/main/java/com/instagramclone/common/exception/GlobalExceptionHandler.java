package com.instagramclone.common.exception;

import com.instagramclone.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiResponse<Object>> handleRateLimitException(
            RateLimitExceededException ex, HttpServletRequest request) {
        log.warn("Rate limit exceeded at {}: {}", request.getRequestURI(), ex.getMessage());
        ApiResponse<Object> response = ApiResponse.error(
                ex.getMessage(),
                ex.getErrorCode(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
                .header("X-RateLimit-Limit", String.valueOf(ex.getLimit()))
                .header("X-RateLimit-Remaining", "0")
                .body(response);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Object>> handleApiException(ApiException ex, HttpServletRequest request) {
        log.warn("Application exception [{}]: {} at {}", ex.getErrorCode(), ex.getMessage(), request.getRequestURI());
        ApiResponse<Object> response = ApiResponse.error(
                ex.getMessage(),
                ex.getErrorCode(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(response, ex.getStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> errors = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.add(fieldError.getField() + ": " + fieldError.getDefaultMessage());
        }
        for (ObjectError globalError : ex.getBindingResult().getGlobalErrors()) {
            errors.add(globalError.getObjectName() + ": " + globalError.getDefaultMessage());
        }
        if (errors.isEmpty()) {
            errors.add("Validation failed on request payload");
        }
        log.warn("Validation failed for request to {}: {}", request.getRequestURI(), errors);
        ApiResponse<Object> response = ApiResponse.error(
                "Validation failed",
                errors,
                request.getRequestURI()
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {
        List<String> errors = new ArrayList<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String property = violation.getPropertyPath() != null ? violation.getPropertyPath().toString() : "request";
            if (property.contains(".")) {
                property = property.substring(property.lastIndexOf('.') + 1);
            }
            errors.add(property + ": " + violation.getMessage());
        }
        if (errors.isEmpty()) {
            errors.add("Validation failed on request parameter");
        }
        log.warn("Constraint violation at {}: {}", request.getRequestURI(), errors);
        ApiResponse<Object> response = ApiResponse.error(
                "Validation failed",
                errors,
                request.getRequestURI()
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Object>> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpServletRequest request) {
        List<String> errors = new ArrayList<>();
        for (var result : ex.getAllValidationResults()) {
            String paramName = result.getMethodParameter().getParameterName();
            for (var resolvableError : result.getResolvableErrors()) {
                errors.add((paramName != null ? paramName : "param") + ": " + resolvableError.getDefaultMessage());
            }
        }
        if (errors.isEmpty()) {
            errors.add("Validation failed on request parameter");
        }
        log.warn("Handler method validation failed at {}: {}", request.getRequestURI(), errors);
        ApiResponse<Object> response = ApiResponse.error(
                "Validation failed",
                errors,
                request.getRequestURI()
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Data integrity violation at {}: {}", request.getRequestURI(), ex.getMessage());
        ApiResponse<Object> response = ApiResponse.error(
                "A data integrity conflict occurred (e.g., duplicate entry or foreign key violation)",
                "DATA_INTEGRITY_VIOLATION",
                request.getRequestURI()
        );
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Malformed HTTP request body at {}: {}", request.getRequestURI(), ex.getMessage());
        ApiResponse<Object> response = ApiResponse.error(
                "Malformed or unreadable JSON request body",
                "MALFORMED_REQUEST",
                request.getRequestURI()
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        log.warn("Parameter type mismatch at {}: param={} value={}", request.getRequestURI(), ex.getName(), ex.getValue());
        ApiResponse<Object> response = ApiResponse.error(
                String.format("Invalid value '%s' for parameter '%s'", ex.getValue(), ex.getName()),
                "INVALID_PARAMETER",
                request.getRequestURI()
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Object>> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {
        log.warn("Uploaded payload exceeds maximum size limit at {}: {}", request.getRequestURI(), ex.getMessage());
        ApiResponse<Object> response = ApiResponse.error(
                "Uploaded file exceeds maximum permitted upload size",
                "PAYLOAD_TOO_LARGE",
                request.getRequestURI()
        );
        return new ResponseEntity<>(response, HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadCredentialsException(
            BadCredentialsException ex, HttpServletRequest request) {
        log.warn("Authentication failed at {}: {}", request.getRequestURI(), ex.getMessage());
        ApiResponse<Object> response = ApiResponse.error(
                "Invalid username or password",
                "INVALID_CREDENTIALS",
                request.getRequestURI()
        );
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<Object>> handleLockedException(
            LockedException ex, HttpServletRequest request) {
        log.warn("Authentication failed due to locked account at {}: {}", request.getRequestURI(), ex.getMessage());
        ApiResponse<Object> response = ApiResponse.error(
                "Invalid username or password",
                "INVALID_CREDENTIALS",
                request.getRequestURI()
        );
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Object>> handleDisabledException(
            DisabledException ex, HttpServletRequest request) {
        log.warn("Authentication failed due to inactive/disabled account at {}: {}", request.getRequestURI(), ex.getMessage());
        ApiResponse<Object> response = ApiResponse.error(
                "Invalid username or password",
                "INVALID_CREDENTIALS",
                request.getRequestURI()
        );
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccountStatusException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccountStatusException(
            AccountStatusException ex, HttpServletRequest request) {
        log.warn("Authentication failed due to account status at {}: {}", request.getRequestURI(), ex.getMessage());
        ApiResponse<Object> response = ApiResponse.error(
                "Invalid username or password",
                "INVALID_CREDENTIALS",
                request.getRequestURI()
        );
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }


    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied at {}: {}", request.getRequestURI(), ex.getMessage());
        ApiResponse<Object> response = ApiResponse.error(
                "You do not have permission to access this resource",
                "ACCESS_DENIED",
                request.getRequestURI()
        );
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneralException(
            Exception ex, HttpServletRequest request) {
        log.error("Unhandled internal error occurred at request {}: ", request.getRequestURI(), ex);
        ApiResponse<Object> response = ApiResponse.error(
                "An unexpected internal error occurred. Please try again later.",
                "INTERNAL_SERVER_ERROR",
                request.getRequestURI()
        );
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
