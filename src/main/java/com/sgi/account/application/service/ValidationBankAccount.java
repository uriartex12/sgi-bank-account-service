package com.sgi.account.application.service;

import com.sgi.account.infrastructure.dto.AccountRequest;
import com.sgi.account.infrastructure.dto.Customer;
import reactor.core.publisher.Mono;

/**
 * Interface for validating bank accounts.
 * Provides methods to validate savings, checking, and fixed-term accounts.
 */
public interface ValidationBankAccount {
    Mono<AccountRequest> savingsAccount(AccountRequest account, Customer customer);
    Mono<AccountRequest> checkingAccount(AccountRequest account, Customer customer);
    Mono<AccountRequest> fixedTermAccount(AccountRequest account, Customer customer);
}
