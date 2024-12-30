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
    @Transactional
    public Mono<TransactionResponse> depositToAccount(String idAccount, Mono<DepositRequest> depositRequest) {
        return bankAccountRepository.findById(idAccount)
                .switchIfEmpty(Mono.defer(() -> Mono.error(new CustomException(CustomError.E_ACCOUNT_NOT_FOUND))))
                .flatMap(account -> depositRequest.flatMap(deposit -> {
                    BigDecimal currentBalance = account.getAccountBalance().getBalance()
                            .add(BigDecimal.valueOf(deposit.getAmount()));
                    TransactionRequest transaction = new TransactionRequest();
                    transaction.setProductId(idAccount);
                    transaction.setAmount(deposit.getAmount());
                    transaction.setType(TransactionRequest.TypeEnum.DEPOSIT);
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
    public Mono<TransactionResponse> withdrawFromAccount(String idAccount, Mono<WithdrawalRequest> withdrawalRequest) {
        return bankAccountRepository.findById(idAccount)
                .switchIfEmpty(Mono.defer(() -> Mono.error(new CustomException(CustomError.E_ACCOUNT_NOT_FOUND))))
                .flatMap(account -> withdrawalRequest
                        .filter(withdrawal -> account.getAccountBalance().getBalance()
                                .compareTo(BigDecimal.valueOf(withdrawal.getAmount())) >= 0)
                        .switchIfEmpty(Mono.error(new CustomException(CustomError.E_INSUFFICIENT_BALANCE)))
                        .flatMap(withdrawal -> {
                            BigDecimal updatedBalance = account.getAccountBalance().getBalance()
                                    .subtract(BigDecimal.valueOf(withdrawal.getAmount()));
                            TransactionRequest transaction = new TransactionRequest();
                            transaction.setProductId(idAccount);
                            transaction.setAmount(withdrawal.getAmount());
                            transaction.setType(TransactionRequest.TypeEnum.WITHDRAWAL);
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
    @Transactional
    public Mono<TransactionResponse> transferFunds(String idAccount, Mono<TransferRequest> transferRequest) {
        return bankAccountRepository.findById(idAccount)
                .switchIfEmpty(Mono.defer(() -> Mono.error(new CustomException(CustomError.E_ACCOUNT_NOT_FOUND))))
                .flatMap(account -> transferRequest
                        .filter(transfer -> account.getAccountBalance().getBalance()
                                .compareTo(BigDecimal.valueOf(transfer.getAmount())) >= 0)
                        .switchIfEmpty(Mono.error(new CustomException(CustomError.E_INSUFFICIENT_BALANCE)))
                        .flatMap(transfer -> bankAccountRepository.findById(transfer.getDestinationProductId())
                                .switchIfEmpty(Mono.defer(() -> Mono.error(new CustomException(CustomError.E_ACCOUNT_NOT_FOUND))))
                                .flatMap(accountDestination -> {
                                    BigDecimal updatedBalanceWithdrawal = account.getAccountBalance().getBalance()
                                            .subtract(BigDecimal.valueOf(transfer.getAmount()));
                                    BigDecimal updatedBalanceDeposit = accountDestination.getAccountBalance().getBalance()
                                            .add(BigDecimal.valueOf(transfer.getAmount()));

                                    TransactionRequest transactionWithdrawal = createTransaction(account,
                                            transfer.getDestinationProductId(),
                                            WITHDRAWAL, updatedBalanceWithdrawal,
                                                    BigDecimal.valueOf(transfer.getAmount()));
                                    TransactionRequest transactionDeposit = createTransaction(accountDestination,
                                            account.getId(), DEPOSIT,
                                            updatedBalanceDeposit,
                                            BigDecimal.valueOf(transfer.getAmount()));

                                    updateAccountBalance(account, updatedBalanceWithdrawal);
                                    updateAccountBalance(accountDestination, updatedBalanceDeposit);

                                    return bankAccountRepository.saveAll(Flux.just(account, accountDestination))
                                            .then()
                                            .flatMap(accountResponse ->
                                            Flux.merge(postTransaction(transactionWithdrawal),
                                                            postTransaction(transactionDeposit))
                                                    .last())
                                            .onErrorResume(e -> {
                                                return bankAccountRepository.saveAll(Flux.just(account, accountDestination))
                                                        .then(Mono.error(new CustomException(CustomError.E_OPERATION_FAILED)));
                                            });
                                })
                        )
                );
    }

    private Mono<TransactionResponse> postTransaction(TransactionRequest transactionRequest) {
        return webClient.post(transactionServiceUrl.concat("/v1/transaction"), transactionRequest, TransactionResponse.class);
    }

    private void updateAccountBalance(BankAccount account, BigDecimal updatedBalance) {
        account.setAccountBalance(Balance.builder()
                .balance(updatedBalance)
                .currency(account.getAccountBalance().getCurrency())
                .build());
    }

    private TransactionRequest createTransaction(BankAccount account, String destinationProductId,
                                                 TypeEnum type,
                                                 BigDecimal updatedBalance, BigDecimal amount) {
        return TransactionExternalMapper.INSTANCE.map(account, destinationProductId, amount, type, updatedBalance);
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
