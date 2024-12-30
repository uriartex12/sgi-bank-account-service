package com.sgi.account.application.service.impl;

import com.sgi.account.domain.model.Balance;
import com.sgi.account.domain.model.BankAccount;
import com.sgi.account.domain.ports.in.TransactionService;
import com.sgi.account.domain.ports.out.BankAccountRepository;
import com.sgi.account.domain.ports.out.FeignExternalService;
import com.sgi.account.domain.shared.CustomError;
import com.sgi.account.infrastructure.exception.CustomException;
import com.sgi.account.infrastructure.mapper.TransactionExternalMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static com.sgi.account.infrastructure.dto.TransactionRequest.TypeEnum.DEPOSIT;
import static com.sgi.account.infrastructure.dto.TransactionRequest.TypeEnum.WITHDRAWAL;

/**
 * Service implementation for managing transactions.
 */
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    @Value("${feign.client.config.transaction-service.url}")
    private String transactionServiceUrl;

    private final BankAccountRepository bankAccountRepository;
    private final FeignExternalService webClient;

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
                                                 TransactionRequest.TypeEnum type,
                                                 BigDecimal updatedBalance, BigDecimal amount) {
        return TransactionExternalMapper.INSTANCE.map(account, destinationProductId, amount, type, updatedBalance);
    }

}
