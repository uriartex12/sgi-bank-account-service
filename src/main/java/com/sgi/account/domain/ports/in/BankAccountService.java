package com.sgi.account.domain.ports.in;

import com.sgi.account.infrastructure.dto.AccountRequest;
import com.sgi.account.infrastructure.dto.AccountResponse;
import com.sgi.account.infrastructure.dto.BalanceResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service interface for managing bank accounts.
 * Defines the operations for creating, updating, deleting, and querying bank accounts and transactions.
 */
public interface BankAccountService {
    Mono<AccountResponse> createAccount(Mono<AccountRequest> customer);
    Mono<Void> deleteAccount(String id);
    Flux<AccountResponse> getAllAccounts();
    Mono<AccountResponse> getAccountById(String id);
    Mono<AccountResponse> updateAccount(String id, Mono<AccountRequest> account);
    Mono<BalanceResponse> getClientBalances(String idAccount);
}
