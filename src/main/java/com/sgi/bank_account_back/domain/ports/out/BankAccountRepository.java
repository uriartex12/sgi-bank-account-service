package com.sgi.bank_account_back.domain.ports.out;
import com.sgi.bank_account_back.domain.model.BankAccount;
import com.sgi.bank_account_back.infrastructure.dto.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BankAccountRepository {
    Mono<AccountResponse> save(BankAccount bankAccount);
    Mono<BankAccount> findById(String id);
    Flux<AccountResponse> findAll();
    Mono<Void> delete(BankAccount bankAccount);
    Mono<Boolean> existsByClientIdAndType(String clientId, String type);
}
