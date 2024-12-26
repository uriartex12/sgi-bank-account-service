package com.sgi.bank_account_back.domain.shared;

import com.sgi.bank_account_back.infrastructure.exception.ApiError;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CustomError {

    E_OPERATION_FAILED(new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, "ACCOUNT-000", "Operation failed")),
    E_INVALID_INPUT(new ApiError(HttpStatus.BAD_REQUEST, "ACCOUNT-100", "Invalid input provided")),
    E_ACCOUNT_NOT_FOUND(new ApiError(HttpStatus.NOT_FOUND, "ACCOUNT-001", "Bank account not found")),
    E_INSUFFICIENT_BALANCE(new ApiError(HttpStatus.PAYMENT_REQUIRED, "ACCOUNT-004", "Insufficient balance")),
    E_MALFORMED_ACCOUNT_DATA(new ApiError(HttpStatus.BAD_REQUEST, "ACCOUNT-003", "Malformed account data")),
    E_MISSING_REQUIRED_ACCOUNT_DATA(new ApiError(HttpStatus.BAD_REQUEST, "ACCOUNT-006", "Missing required account data")),
    E_DUPLICATE_ACCOUNT_NUMBER(new ApiError(HttpStatus.CONFLICT, "ACCOUNT-005", "Account with this number already exists")),
    E_MAX_SAVINGS_ACCOUNTS_REACHED(new ApiError(HttpStatus.BAD_REQUEST, "ACCOUNT-007", "The personal client cannot have more than one bank account.")),
    E_BUSINESS_CLIENT_CANNOT_HAVE_SAVINGS(new ApiError(HttpStatus.BAD_REQUEST, "ACCOUNT-010", "Business clients cannot have savings accounts.")),
    E_BUSINESS_CLIENT_CANNOT_HAVE_FIXED_TERM(new ApiError(HttpStatus.BAD_REQUEST, "ACCOUNT-012", "Business clients cannot have fixed-term accounts."));

    private final ApiError error;
}
