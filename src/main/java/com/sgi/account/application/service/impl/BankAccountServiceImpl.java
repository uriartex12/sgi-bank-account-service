package com.sgi.account.application.service.impl;

import com.sgi.account.application.service.ValidationBankAccount;
import com.sgi.account.domain.model.Balance;
import com.sgi.account.domain.model.BankAccount;
import com.sgi.account.domain.ports.in.BankAccountService;
import com.sgi.account.domain.ports.out.BankAccountRepository;
import com.sgi.account.domain.ports.out.FeignExternalService;
import com.sgi.account.domain.shared.CustomError;
import com.sgi.account.infrastructure.dto.AccountRequest;
import com.sgi.account.infrastructure.dto.AccountResponse;
import com.sgi.account.infrastructure.dto.TransactionResponse;
import com.sgi.account.infrastructure.dto.TransactionRequest;
import com.sgi.account.infrastructure.dto.BalanceResponse;
import com.sgi.account.infrastructure.dto.CustomerResponse;
import com.sgi.account.infrastructure.exception.CustomException;
import com.sgi.account.infrastructure.mapper.BankAccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Random;

/**
 * Service implementation for managing credits.
 */
@Service
@RequiredArgsConstructor
public class BankAccountServiceImpl implements BankAccountService {

    @Value("${feign.client.config.transaction-service.url}")
    private String transactionServiceUrl;

    @Value("${feign.client.config.customer-service.url}")
    private String customerServiceUrl;

    private final BankAccountRepository bankAccountRepository;
    private final FeignExternalService webClient;
    private final ValidationBankAccount validateSavingsAccount;

    @Override
    public Mono<AccountResponse> createAccount(Mono<AccountRequest> accountRequest) {
        return accountRequest.flatMap(account ->
                webClient.getMono(
                                customerServiceUrl.concat("/v1/customers/{id}"),
                                account.getClientId(),
                                CustomerResponse.class
                        )
                        .flatMap(customerResponse ->
                                validateBankAccount(account, customerResponse.getType().name())
                                        .flatMap(validAccount -> {
                                            BankAccount bankAccount = BankAccountMapper.INSTANCE.toAccount(validAccount);
                                            bankAccount.setAccountBalance(new Balance(
                                                    validAccount.getBalance() == null ? BigDecimal.ZERO
                                                            : validAccount.getBalance(),
                                                    validAccount.getCurrency()
                                            ));
                                            bankAccount.setAccountNumber(generateAccountNumber());
                                            bankAccount.setCreatedDate(Instant.now());
                                            bankAccount.setUpdatedDate(Instant.now());
                                            return bankAccountRepository.save(bankAccount);
                                        })
                                        .onErrorResume(e -> Mono.error(new Exception("Invalid account data", e)))
                        )
                        .onErrorResume(e -> Mono.error(new Exception("Failed to fetch customer", e)))
        );
    }

    private Mono<AccountRequest> validateBankAccount(AccountRequest account, String customerType) {
        return Mono.just(account)
                .flatMap(acc -> switch (acc.getType()) {
                    case SAVINGS -> validateSavingsAccount.savingsAccount(acc, customerType);
                    case CHECKING -> validateSavingsAccount.checkingAccount(acc, customerType);
                    case FIXED_TERM -> validateSavingsAccount.fixedTermAccount(acc, customerType);
                });
    }

    @Override
    public Mono<Void> deleteAccount(String id) {
        return bankAccountRepository.findById(id)
                .flatMap(bankAccountRepository::delete)
                .switchIfEmpty(Mono.error(new CustomException(CustomError.E_ACCOUNT_NOT_FOUND)));
    }

    @Override
    public Flux<AccountResponse> getAllAccounts() {
        return bankAccountRepository.findAll();
    }

    @Override
    public Mono<AccountResponse> getAccountById(String id) {
        return bankAccountRepository.findById(id)
                .map(BankAccountMapper.INSTANCE::toAccountResponse);
    }

    @Override
    public Mono<AccountResponse> updateAccount(String id, Mono<AccountRequest> bankAccount) {
        return bankAccountRepository.findById(id)
                .switchIfEmpty(Mono.error(new CustomException(CustomError.E_ACCOUNT_NOT_FOUND)))
                .flatMap(account ->
                        bankAccount.map(updatedAccount -> {
                            account.setType(updatedAccount.getType().getValue());
                            account.setAuthorizedSigners(updatedAccount.getAuthorizedSigners());
                            account.setHolders(updatedAccount.getHolders());
                            account.setUpdatedDate(Instant.now());
                            return account;
                        })
                ).flatMap(bankAccountRepository::save);
    }

    @Override
    @Transactional
    public Mono<TransactionResponse> depositToAccount(String idAccount, Mono<TransactionRequest> transactionRequest) {
        return bankAccountRepository.findById(idAccount)
                .switchIfEmpty(Mono.defer(() -> Mono.error(new CustomException(CustomError.E_ACCOUNT_NOT_FOUND))))
                .flatMap(account -> transactionRequest.flatMap(transaction -> {
                    BigDecimal currentBalance = account.getAccountBalance().getBalance()
                            .add(BigDecimal.valueOf(transaction.getAmount()));
                    transaction.setClientId(account.getClientId());
                    transaction.setBalance(currentBalance.doubleValue());
                    account.setAccountBalance(Balance.builder()
                            .balance(currentBalance)
                            .currency(account.getAccountBalance().getCurrency())
                            .build());
                    return bankAccountRepository.save(account)
                            .flatMap(savedAccount -> webClient.post(
                                    transactionServiceUrl.concat("/v1/transaction"),
                                    transaction,
                                    TransactionResponse.class
                            ));
                }));
    }

    @Override
    @Transactional
    public Mono<TransactionResponse> withdrawFromAccount(String idAccount, Mono<TransactionRequest> transactionRequest) {
        return bankAccountRepository.findById(idAccount)
                .switchIfEmpty(Mono.defer(() -> Mono.error(new CustomException(CustomError.E_ACCOUNT_NOT_FOUND))))
                .flatMap(account -> transactionRequest
                        .filter(transaction -> account.getAccountBalance().getBalance()
                                .compareTo(BigDecimal.valueOf(transaction.getAmount())) >= 0)
                        .switchIfEmpty(Mono.error(new CustomException(CustomError.E_INSUFFICIENT_BALANCE)))
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
                                    .flatMap(savedAccount -> webClient.post(
                                            transactionServiceUrl.concat("/v1/transaction"),
                                            transaction,
                                            TransactionResponse.class
                                    ));
                        }));
    }

    @Override
    public Mono<BalanceResponse> getClientBalances(String idAccount) {
        return bankAccountRepository.findById(idAccount)
                .map(BankAccountMapper.INSTANCE::toBalance);
    }

    @Override
    public Flux<TransactionResponse> getClientTransactions(String idAccount) {
        return bankAccountRepository.findById(idAccount)
                .switchIfEmpty(Mono.error(new CustomException(CustomError.E_ACCOUNT_NOT_FOUND)))
                .flatMapMany(credit -> webClient.getFlux(
                        transactionServiceUrl.concat("/v1/{productId}/transaction"),
                        idAccount,
                        TransactionResponse.class
                ));
    }

    private String generateAccountNumber() {
        return String.format("%04d00%012d", new Random().nextInt(10000), new Random().nextLong(1000000000000L));
    }
}
