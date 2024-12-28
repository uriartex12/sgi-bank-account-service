package com.sgi.account.application.service;

import com.sgi.account.infrastructure.dto.AccountRequest;
import reactor.core.publisher.Mono;

/**
 * Interface for validating bank accounts.
 * Provides methods to validate savings, checking, and fixed-term accounts.
 */
public interface ValidationBankAccount {
    Mono<AccountRequest> savingsAccount(AccountRequest account, String customerType);
    Mono<AccountRequest> checkingAccount(AccountRequest account, String customerType);
    Mono<AccountRequest> fixedTermAccount(AccountRequest account, String customerType);
}
