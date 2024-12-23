package com.sgi.bank_account_back.application.service;

import com.sgi.bank_account_back.domain.ports.in.BankAccountService;
import com.sgi.bank_account_back.domain.ports.out.BankAccountRepository;
import com.sgi.bank_account_back.infrastructure.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class BankAccountServiceImpl implements BankAccountService {

    private final BankAccountRepository bankAccountRepository;

    public BankAccountServiceImpl(BankAccountRepository bankAccountRepository) {
        this.bankAccountRepository = bankAccountRepository;
    }

    @Override
    public Mono<ResponseEntity<AccountResponse>> createAccount(Mono<AccountRequest> accountMono){
        return  bankAccountRepository.createAccount(accountMono)
                .map(createdAccount ->ResponseEntity.ok().body(createdAccount))
                .onErrorResume(error -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()));
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteAccount(String id) {
        return bankAccountRepository.deleteAccount(id)
                .map(deletBankAccount-> ResponseEntity.ok().body(deletBankAccount))
                .onErrorResume(error -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()));
    }

    @Override
    public Mono<ResponseEntity<Flux<AccountResponse>>> getAllAccounts() {
        return Mono.fromSupplier(() -> ResponseEntity.ok().body(bankAccountRepository.getAllAccounts()))
                .onErrorResume(error -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()));
    }

    @Override
    public Mono<ResponseEntity<AccountResponse>> getAccountById(String id) {
        return  bankAccountRepository.getAccountById(id)
                .map(bankAccount ->ResponseEntity.ok().body(bankAccount))
                .onErrorResume(error -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()));
    }

    @Override
    public Mono<ResponseEntity<AccountResponse>> updateAccount(String id, Mono<AccountRequest> account) {
        return  bankAccountRepository.updateAccount(id,account)
                .map(bankAccount ->ResponseEntity.ok().body(bankAccount))
                .onErrorResume(error -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()));

    }

    @Override
    public Mono<ResponseEntity<TransactionResponse>> depositToAccount(String idAccount, Mono<TransactionRequest> transactionRequest) {
        return bankAccountRepository.depositToAccount(idAccount, transactionRequest)
                .map(bankAccount ->ResponseEntity.ok().body(bankAccount))
                .onErrorResume(error -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()));
    }

    @Override
    public Mono<ResponseEntity<TransactionResponse>> withdrawFromAccount(String idAccount, Mono<TransactionRequest> transactionRequest) {
        return bankAccountRepository.withdrawFromAccount(idAccount, transactionRequest)
                .map(bankAccount ->ResponseEntity.ok().body(bankAccount))
                .onErrorResume(error -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()));
    }

    @Override
    public Mono<ResponseEntity<BalanceResponse>> getClientBalances(String idAccount) {
        return bankAccountRepository.getClientBalances(idAccount)
                .map(bankAccount ->ResponseEntity.ok().body(bankAccount))
                .onErrorResume(error -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()));
    }



    @Override
    public Mono<ResponseEntity<Flux<TransactionResponse>>> getClientTransactions(String idAccount) {
        return bankAccountRepository.getClientTransactions(idAccount)
                .collectList()
                .map(transactions -> ResponseEntity.ok().body(Flux.fromIterable(transactions)))
                .onErrorResume(error -> {
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Flux.empty()));
                });
    }

}
