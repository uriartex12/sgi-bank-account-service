package com.sgi.account.infrastructure.repository.impl;

import com.sgi.account.domain.model.BankAccount;
import com.sgi.account.domain.ports.out.BankAccountRepository;
import com.sgi.account.infrastructure.dto.AccountResponse;
import com.sgi.account.infrastructure.mapper.BankAccountMapper;
import com.sgi.account.infrastructure.repository.BankAccountRepositoryJpa;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Implementation of the {@link BankAccountRepository} interface.
 * Provides operations for managing bank accounts using a JPA-based repository.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class BankAccountRepositoryImpl implements BankAccountRepository {

    private final BankAccountRepositoryJpa repositoryJpa;

    @Override
    public Mono<AccountResponse> save(BankAccount bankAccount) {
        return repositoryJpa.save(bankAccount)
                .map(BankAccountMapper.INSTANCE::toAccountResponse);
    }

    @Override
    public Mono<BankAccount> findById(String id) {
        return repositoryJpa.findById(id);
    }

    @Override
    public Flux<AccountResponse> findAll() {
        return repositoryJpa.findAll()
               .map(BankAccountMapper.INSTANCE::toAccountResponse);
    }

    @Override
    public Mono<Void> delete(BankAccount bankAccount) {
        return repositoryJpa.delete(bankAccount);
    }

    @Override
    public Mono<Boolean> existsByClientIdAndType(String clientId, String type) {
        return repositoryJpa.existsByClientIdAndType(clientId, type);
    }
}