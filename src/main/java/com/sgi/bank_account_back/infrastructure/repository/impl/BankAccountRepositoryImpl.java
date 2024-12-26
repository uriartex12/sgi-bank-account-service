package com.sgi.bank_account_back.infrastructure.repository.impl;
import com.sgi.bank_account_back.domain.model.BankAccount;
import com.sgi.bank_account_back.domain.ports.out.BankAccountRepository;
import com.sgi.bank_account_back.infrastructure.dto.AccountResponse;
import com.sgi.bank_account_back.infrastructure.mapper.BankAccountMapper;
import com.sgi.bank_account_back.infrastructure.repository.BankAccountRepositoryJPA;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Repository
@RequiredArgsConstructor
public class BankAccountRepositoryImpl implements BankAccountRepository {

    private final BankAccountRepositoryJPA repositoryJPA;

    @Override
    public Mono<AccountResponse> save(BankAccount bankAccount) {
        return repositoryJPA.save(bankAccount)
                .map(BankAccountMapper.INSTANCE::map);
    }

    @Override
    public Mono<BankAccount> findById(String id) {
        return repositoryJPA.findById(id);
    }

    @Override
    public Flux<AccountResponse> findAll() {
        return repositoryJPA.findAll() 
               .map(BankAccountMapper.INSTANCE::map);
    }

    @Override
    public Mono<Void> delete(BankAccount bankAccount) {
        return repositoryJPA.delete(bankAccount);
    }

    @Override
    public Mono<Boolean> existsByClientIdAndType(String clientId, String type) {
        return repositoryJPA.existsByClientIdAndType(clientId, type);
    }
}