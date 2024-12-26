package com.sgi.bank_account_back.application.service;

import com.sgi.bank_account_back.infrastructure.dto.AccountRequest;
import reactor.core.publisher.Mono;

public interface ValidationBankAccount {
    Mono<AccountRequest> savingsAccount(AccountRequest account, String customerType);
    Mono<AccountRequest> checkingAccount(AccountRequest account, String customerType);
    Mono<AccountRequest> fixedTermAccount(AccountRequest account, String customerType);
}
