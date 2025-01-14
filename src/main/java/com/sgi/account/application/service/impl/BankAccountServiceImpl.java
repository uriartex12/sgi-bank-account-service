package com.sgi.account.application.service.impl;

import com.sgi.account.application.service.ValidationBankAccount;
import com.sgi.account.domain.model.Balance;
import com.sgi.account.domain.model.BankAccount;
import com.sgi.account.domain.ports.in.BankAccountService;
import com.sgi.account.domain.ports.out.BankAccountRepository;
import com.sgi.account.domain.ports.out.FeignExternalService;
import com.sgi.account.domain.shared.Constants;
import com.sgi.account.domain.shared.CustomError;
import com.sgi.account.infrastructure.dto.AccountRequest;
import com.sgi.account.infrastructure.dto.AccountResponse;
import com.sgi.account.infrastructure.dto.AccountBalanceResponse;
import com.sgi.account.infrastructure.dto.BalanceResponse;
import com.sgi.account.infrastructure.dto.BalanceRequest;
import com.sgi.account.infrastructure.dto.Customer;


import com.sgi.account.infrastructure.exception.CustomException;
import com.sgi.account.infrastructure.mapper.BankAccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import java.time.Instant;

import static com.sgi.account.domain.shared.Constants.COMPLETED;

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
                                customerServiceUrl.concat("/v1/customers/{customerId}"),
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
                                            bankAccount.setAccountNumber(Constants.generateAccountNumber());
                                            bankAccount.setCreatedDate(Instant.now());
                                            bankAccount.setUpdatedDate(Instant.now());
                                            bankAccount.setMovementsUsed(BigDecimal.ZERO.intValue());
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
                .switchIfEmpty(Mono.error(new CustomException(CustomError.E_ACCOUNT_NOT_FOUND)))
                .flatMap(bankAccountRepository::delete);
    }

    @Override
    public Flux<AccountResponse> getAllAccounts(String clientId, String type, String accountId) {
        return bankAccountRepository.findAll(clientId, type, accountId);
    }

    @Override
    public Mono<AccountResponse> getAccountById(String id) {
        return bankAccountRepository.findById(id)
                .switchIfEmpty(Mono.error(new CustomException(CustomError.E_ACCOUNT_NOT_FOUND)))
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

    @Override
    public Mono<AccountBalanceResponse> updatedBalanceByAccountId(String action, Mono<BalanceRequest> balanceRequestMono) {
        return balanceRequestMono.flatMap(balanceRequest ->
                bankAccountRepository.findById(balanceRequest.getAccountId())
                        .switchIfEmpty(Mono.defer(() -> Mono.error(new CustomException(CustomError.E_ACCOUNT_NOT_FOUND))))
                        .flatMap(bankAccount -> processBalanceUpdate(action, balanceRequest, bankAccount))
        );
    }

    private Mono<AccountBalanceResponse> processBalanceUpdate(String action, BalanceRequest balanceRequest, BankAccount bankAccount) {
        return switch (action) {
            case "deduct" -> processDeduction(balanceRequest, bankAccount);
            case "add" -> processAddition(balanceRequest, bankAccount);
            default -> Mono.error(new CustomException(CustomError.E_INVALID_ACTION));
        };
    }

    private Mono<AccountBalanceResponse> processDeduction(BalanceRequest balanceRequest, BankAccount bankAccount) {
        return Mono.just(bankAccount)
                .filter(account -> account.getAccountBalance().getBalance().compareTo(balanceRequest.getAmount()) >= 0)
                .switchIfEmpty(Mono.error(new CustomException(CustomError.E_INSUFFICIENT_BALANCE)))
                .flatMap(account -> updateBalance(account, balanceRequest.getAmount().negate()));
    }

    private Mono<AccountBalanceResponse> processAddition(BalanceRequest balanceRequest, BankAccount bankAccount) {
        return updateBalance(bankAccount, balanceRequest.getAmount());
    }

    private Mono<AccountBalanceResponse> updateBalance(BankAccount bankAccount, BigDecimal amount) {
        bankAccount.setAccountBalance(Balance.builder()
                .balance(bankAccount.getAccountBalance().getBalance().add(amount))
                .currency(bankAccount.getAccountBalance().getCurrency())
                .build());
        return bankAccountRepository.save(bankAccount)
                .map(element -> BankAccountMapper.INSTANCE.toAccountBalance(element, COMPLETED));
    }

}
