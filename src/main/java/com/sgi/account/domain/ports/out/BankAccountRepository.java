package com.sgi.account.domain.ports.out;

import com.sgi.account.domain.model.BankAccount;
import com.sgi.account.infrastructure.dto.AccountResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository interface defining operations to manage credits.
 */
public interface BankAccountRepository {

    Mono<AccountResponse> save(BankAccount bankAccount);

    Flux<AccountResponse> saveAll(Flux<BankAccount> bankAccounts);

    Mono<BankAccount> findById(String id);

    Flux<AccountResponse> findAll(String clientId, String type, String accountId);

    Mono<Void> delete(BankAccount bankAccount);

    Mono<Boolean> existsByClientIdAndType(String clientId, String type);

}
