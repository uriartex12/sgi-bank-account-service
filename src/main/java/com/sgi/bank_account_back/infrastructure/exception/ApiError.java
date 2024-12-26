package com.sgi.bank_account_back.infrastructure.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
@AllArgsConstructor
public class ApiError {
    private final HttpStatus status;
    private final String code;
    private final String message;
}
