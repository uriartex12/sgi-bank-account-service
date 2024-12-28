package com.sgi.account.infrastructure.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;

/**
 * Represents an API error with an HTTP status code, error code, and message.
 * Used to standardize error responses in the application.
 */
@Data
@AllArgsConstructor
public class ApiError {
    private final HttpStatus status;
    private final String code;
    private final String message;
}
