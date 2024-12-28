package com.sgi.account.infrastructure.exception;

import com.sgi.account.domain.shared.CustomError;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Custom exception class that extends RuntimeException to handle error details.
 * It includes information such as status, message, code, and timestamp.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CustomException extends RuntimeException {

    /** HTTP status code associated with the error. */
    private Integer status;

    /** Error message. */
    private String message;

    /** Error code. */
    private String code;

    /** Timestamp when the exception was thrown. */
    private LocalDateTime timestamp;

    /**
     * Constructor for creating a CustomException based on a CustomError object.
     *
     * @param error The CustomError object containing error details.
     */
    public CustomException(CustomError error) {
        this.status = error.getError().getStatus().value();
        this.message = error.getError().getMessage();
        this.code = error.getError().getCode();
        this.timestamp = LocalDateTime.now();
    }
}
