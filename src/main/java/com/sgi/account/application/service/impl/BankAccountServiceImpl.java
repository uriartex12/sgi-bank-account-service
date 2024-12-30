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
import com.sgi.account.infrastructure.dto.BalanceResponse;
import com.sgi.account.infrastructure.dto.TransactionRequest;
import com.sgi.account.infrastructure.dto.TransactionResponse;
import com.sgi.account.infrastructure.dto.DepositRequest;
import com.sgi.account.infrastructure.dto.WithdrawalRequest;
import com.sgi.account.infrastructure.dto.Customer;
import com.sgi.account.infrastructure.dto.TransferRequest;
import com.sgi.account.infrastructure.exception.CustomException;
import com.sgi.account.infrastructure.mapper.BankAccountMapper;
import com.sgi.account.infrastructure.mapper.TransactionExternalMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Random;

import static com.sgi.account.infrastructure.dto.TransactionRequest.TypeEnum;
import static com.sgi.account.infrastructure.dto.TransactionRequest.TypeEnum.DEPOSIT;
import static com.sgi.account.infrastructure.dto.TransactionRequest.TypeEnum.WITHDRAWAL;


/**
 * Service implementation for managing Bank Account.
 */
@Service
@RequiredArgsConstructor
public class BankAccountServiceImpl implements BankAccountService {

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
                                Customer.class
                        )
                        .flatMap(customerResponse ->
                                validateBankAccount(account, customerResponse)
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

    private Mono<AccountRequest> validateBankAccount(AccountRequest account, Customer customer) {
        return Mono.just(account)
                .flatMap(acc -> switch (acc.getType()) {
                    case SAVINGS -> validateSavingsAccount.savingsAccount(acc, customer);
                    case CHECKING -> validateSavingsAccount.checkingAccount(acc, customer);
                    case FIXED_TERM -> validateSavingsAccount.fixedTermAccount(acc, customer);
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
    public Mono<BalanceResponse> getClientBalances(String idAccount) {
        return bankAccountRepository.findById(idAccount)
                .map(BankAccountMapper.INSTANCE::toBalance);
    }

    private String generateAccountNumber() {
        return String.format("%04d00%012d", new Random().nextInt(10000), new Random().nextLong(1000000000000L));
    }
}
