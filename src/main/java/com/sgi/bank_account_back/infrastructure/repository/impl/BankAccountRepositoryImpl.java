package com.sgi.bank_account_back.infrastructure.repository.impl;

import com.sgi.bank_account_back.domain.model.Balance;
import com.sgi.bank_account_back.domain.model.BankAccount;
import com.sgi.bank_account_back.domain.ports.out.BankAccountRepository;
import com.sgi.bank_account_back.infrastructure.dto.*;
import com.sgi.bank_account_back.infrastructure.mapper.BankAccountMapper;
import com.sgi.bank_account_back.infrastructure.repository.BankAccountRepositoryJPA;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Random;
import static com.sgi.bank_account_back.infrastructure.dto.AccountRequest.TypeEnum.*;

@Repository
@Slf4j
public class BankAccountRepositoryImpl implements BankAccountRepository {

    @Value("${feign.client.config.transaction-service.url}")
    private String transactionServiceUrl;
    private final WebClient.Builder webClientBuilder;
    private final BankAccountRepositoryJPA bankAccountRepository;
    private WebClient webClient;

    public BankAccountRepositoryImpl(WebClient.Builder webClientBuilder, BankAccountRepositoryJPA bankAccountRepository) {
        this.webClientBuilder = webClientBuilder;
        this.bankAccountRepository = bankAccountRepository;
    }

    @PostConstruct
    public void init() {
        this.webClient = webClientBuilder.baseUrl(transactionServiceUrl).build();
    }


    @Override
    public Mono<AccountResponse> createAccount(Mono<AccountRequest> accountRequest) {
        return accountRequest.flatMap(account ->
                validateAccount(account)
                    .flatMap(validAccount -> {
                        BankAccount bankAccount = BankAccountMapper.INSTANCE.map(validAccount);
                        bankAccount.setAccountBalance(new Balance(
                                validAccount.getBalance() == null ? BigDecimal.ZERO : validAccount.getBalance(),
                                validAccount.getCurrency()));
                        bankAccount.setAccountNumber(generateAccountNumber());
                        bankAccount.setCreatedDate(Instant.now());
                        bankAccount.setUpdatedDate(Instant.now());
                        return bankAccountRepository.save(bankAccount)
                                .map(BankAccountMapper.INSTANCE::map);
                    })
        );
    }

    private Mono<AccountRequest> validateAccount(AccountRequest account) {
        return Mono.just(account)
                .flatMap(acc -> switch (acc.getType()) {
                    case SAVINGS -> validateSavingsAccount(acc);
                    case CHECKING -> validateCheckingAccount(acc);
                    case FIXED_TERM -> validateFixedTermAccount(acc);
                });
    }

    @Override
    public Mono<Void> deleteAccount(String id) {
        return bankAccountRepository.findById(id)
                .flatMap(bankAccountRepository::delete)
                .switchIfEmpty(Mono.error(new Exception("Bank Account not found")));
    }

    @Override
    public Flux<AccountResponse> getAllAccounts() {
        return bankAccountRepository.findAll()
                .map(BankAccountMapper.INSTANCE::map);
    }

    @Override
    public Mono<AccountResponse> getAccountById(String id) {
        return bankAccountRepository.findById(id)
                .map(BankAccountMapper.INSTANCE::map);
    }

    @Override
    public Mono<AccountResponse> updateAccount(String id, Mono<AccountRequest> customer) {
        return bankAccountRepository.findById(id)
                .switchIfEmpty(Mono.error(new Exception("Bank Account not found")))
                .flatMap(accountRequest ->
                        customer.map(updatedAccount -> {
                            BankAccount updatedEntity = BankAccountMapper.INSTANCE.map(updatedAccount);
                            updatedEntity.setUpdatedDate(Instant.now());
                            updatedEntity.setId(accountRequest.getId());
                            return updatedEntity;
                        })
                )
                .flatMap(bankAccountRepository::save)
                .map(BankAccountMapper.INSTANCE::map);
    }

    @Override
    @Transactional
    public Mono<TransactionResponse> depositToAccount(String idAccount, Mono<TransactionRequest> transactionRequest) {
        return bankAccountRepository.findById(idAccount)
                .switchIfEmpty(Mono.defer(() -> Mono.error(new Exception("Bank Account Not Found: "+idAccount))))
                .flatMap(account -> transactionRequest.flatMap(transaction -> {
                    BigDecimal currentBalance = account.getAccountBalance().getBalance().add(BigDecimal.valueOf(transaction.getAmount()));
                    transaction.setClientId(account.getClientId());
                    transaction.setBalance(currentBalance.doubleValue());
                    account.setAccountBalance(Balance.builder()
                            .balance(currentBalance)
                            .currency(account.getAccountBalance().getCurrency())
                            .build());
                    return bankAccountRepository.save(account)
                            .flatMap(savedAccount -> webClient.post()
                                    .uri("/v1/transaction")
                                    .bodyValue(transaction)
                                    .retrieve()
                                    .bodyToMono(TransactionResponse.class)
                                    .doOnNext(response -> log.info("Transaction saved successfully: {}", response))
                                    .onErrorResume(ex -> {
                                        log.error("Error during transaction process", ex);
                                        return Mono.error(new Exception("Error processing transaction", ex));
                                    }));
                }));
    }

    @Override
    @Transactional
    public Mono<TransactionResponse> withdrawFromAccount(String idAccount, Mono<TransactionRequest> transactionRequest) {
        return bankAccountRepository.findById(idAccount)
                .switchIfEmpty(Mono.defer(() -> Mono.error(new Exception("Bank Account Not Found: " + idAccount))))
                .flatMap(account -> transactionRequest
                        .filter(transaction -> account.getAccountBalance().getBalance()
                                .compareTo(BigDecimal.valueOf(transaction.getAmount())) >= 0)
                        .switchIfEmpty(Mono.error(new Exception("Insufficient balance for transaction")))
                        .flatMap(transaction -> {
                            BigDecimal updatedBalance = account.getAccountBalance().getBalance()
                                    .subtract(BigDecimal.valueOf(transaction.getAmount()));
                            transaction.setClientId(account.getClientId());
                            transaction.setBalance(updatedBalance.doubleValue());
                            account.setAccountBalance(Balance.builder()
                                    .balance(updatedBalance)
                                    .currency(account.getAccountBalance().getCurrency())
                                    .build());
                            return bankAccountRepository.save(account)
                                    .flatMap(savedAccount -> webClient.post()
                                            .uri("/v1/transaction")
                                            .bodyValue(transaction)
                                            .retrieve()
                                            .bodyToMono(TransactionResponse.class)
                                            .doOnNext(response -> log.info("Transaction saved successfully: {}", response))
                                            .onErrorResume(ex -> {
                                                log.error("Error during transaction process", ex);
                                                return Mono.error(new Exception("Error processing transaction", ex));
                                            }));
                        }));
    }

    @Override
    public Mono<BalanceResponse> getClientBalances(String idAccount) {
        return bankAccountRepository.findById(idAccount)
                .map(BankAccountMapper.INSTANCE::balance);
    }

    @Override
    public Flux<TransactionResponse> getClientTransactions(String idAccount) {
        return bankAccountRepository.findById(idAccount)
                .switchIfEmpty(Mono.error(new Exception("Bank Account Not Found: " + idAccount)))
                .flatMapMany(account -> webClient.get()
                        .uri("/v1/{accountId}/transaction", account.getId())
                        .retrieve()
                        .bodyToFlux(TransactionResponse.class)
                        .doOnNext(response -> log.info("Transaction: {}", response))
                        .onErrorResume(ex -> {
                            log.error("Error during transaction process", ex);
                            return Flux.error(new Exception("Error processing list transactions", ex));
                        }));
    }

    private String generateAccountNumber() {
        return String.format("%04d00%012d", new Random().nextInt(10000), new Random().nextLong(1000000000000L));
    }

    private Mono<AccountRequest> validateSavingsAccount(AccountRequest account) {
        if (account.getMovementLimit() == null || account.getMovementLimit() <= 0) {
            return Mono.error(new IllegalArgumentException("Savings account must have a positive movement limit."));
        }
        account.setMaintenanceFee(BigDecimal.ZERO);
        return Mono.just(account);
    }
    private Mono<AccountRequest> validateCheckingAccount(AccountRequest account) {
        if (account.getMaintenanceFee() == null || account.getMaintenanceFee().compareTo(BigDecimal.ZERO) <= 0) {
            return Mono.error(new IllegalArgumentException("Checking account must have a positive maintenance fee."));
        }
        account.setMovementLimit(null);
        return Mono.just(account);
    }
    private Mono<AccountRequest> validateFixedTermAccount(AccountRequest account) {
        if (account.getTransactionDay() == null) {
            return Mono.error(new IllegalArgumentException("Fixed-term account must have a transaction day."));
        }
        account.setMaintenanceFee(BigDecimal.ZERO);
        account.setMovementLimit(1);
        return Mono.just(account);
    }


}