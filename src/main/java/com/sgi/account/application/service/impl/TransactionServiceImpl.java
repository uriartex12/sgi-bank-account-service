package com.sgi.account.application.service.impl;

import com.sgi.account.domain.model.Balance;
import com.sgi.account.domain.model.BankAccount;
import com.sgi.account.domain.ports.in.TransactionService;
import com.sgi.account.domain.ports.out.BankAccountRepository;
import com.sgi.account.domain.ports.out.FeignExternalService;
import com.sgi.account.domain.shared.CustomError;
import com.sgi.account.infrastructure.dto.DepositRequest;
import com.sgi.account.infrastructure.dto.TransactionRequest;
import com.sgi.account.infrastructure.dto.TransactionResponse;
import com.sgi.account.infrastructure.dto.WithdrawalRequest;
import com.sgi.account.infrastructure.dto.TransferRequest;
import com.sgi.account.infrastructure.dto.AccountRequest;
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
    public Flux<TransactionResponse> getAccountIdTransactions(String idAccount) {
        return bankAccountRepository.findById(idAccount)
                .switchIfEmpty(Mono.error(new CustomException(CustomError.E_ACCOUNT_NOT_FOUND)))
                .flatMapMany(credit -> webClient.getFlux(
                        transactionServiceUrl.concat("/v1/transactions/{productId}/card"),
                        idAccount,
                        TransactionResponse.class
                ));
    }

    @Override
    @Transactional
    public Mono<TransactionResponse> depositToAccount(String idAccount, Mono<DepositRequest> depositRequest) {
        return bankAccountRepository.findById(idAccount)
                .switchIfEmpty(Mono.defer(() -> Mono.error(new CustomException(CustomError.E_ACCOUNT_NOT_FOUND))))
                .flatMap(account -> depositRequest
                        .filter(deposit -> account.getAccountBalance().getBalance()
                                .add(BigDecimal.valueOf(deposit.getAmount()))
                                .compareTo(calculateCommission(account)) >= 0)
                        .switchIfEmpty(Mono.error(new CustomException(CustomError.E_INSUFFICIENT_BALANCE)))
                        .flatMap(deposit -> {
                            BigDecimal commission = calculateCommission(account);
                            BigDecimal currentBalance = account.getAccountBalance().getBalance()
                                    .add(BigDecimal.valueOf(deposit.getAmount()));
                            TransactionRequest transaction = new TransactionRequest();
                            transaction.setProductId(idAccount);
                            transaction.setAmount(deposit.getAmount());
                            transaction.setType(DEPOSIT);
                            transaction.setClientId(account.getClientId());
                            transaction.setBalance(currentBalance.subtract(commission).doubleValue());
                            transaction.setCommission(commission.doubleValue());
                            account.setMovementsUsed(account.getMovementsUsed() + 1);
                            account.setAccountBalance(Balance.builder()
                                    .balance(currentBalance.subtract(commission))
                                    .currency(account.getAccountBalance().getCurrency())
                                    .build());
                    return bankAccountRepository.save(account)
                            .flatMap(savedAccount -> postTransaction(transaction));
                }));
    }

    @Override
    @Transactional
    public Mono<TransactionResponse> withdrawFromAccount(String idAccount, Mono<WithdrawalRequest> withdrawalRequest) {
        return bankAccountRepository.findById(idAccount)
                .switchIfEmpty(Mono.defer(() -> Mono.error(new CustomException(CustomError.E_ACCOUNT_NOT_FOUND))))
                .flatMap(account -> withdrawalRequest
                        .filter(withdrawal -> account.getAccountBalance().getBalance()
                                .compareTo(BigDecimal.valueOf(withdrawal.getAmount()).add(calculateCommission(account))) >= 0)
                        .switchIfEmpty(Mono.error(new CustomException(CustomError.E_INSUFFICIENT_BALANCE)))
                        .flatMap(withdrawal -> {
                            BigDecimal commission = calculateCommission(account);
                            BigDecimal updatedBalance = account.getAccountBalance().getBalance()
                                    .subtract(commission)
                                    .subtract(BigDecimal.valueOf(withdrawal.getAmount()));
                            TransactionRequest transaction = new TransactionRequest();
                            transaction.setProductId(idAccount);
                            transaction.setAmount(withdrawal.getAmount());
                            transaction.setType(WITHDRAWAL);
                            transaction.setCommission(commission.doubleValue());
                            transaction.setClientId(account.getClientId());
                            transaction.setBalance(updatedBalance.doubleValue());
                            account.setAccountBalance(Balance.builder()
                                    .balance(updatedBalance)
                                    .currency(account.getAccountBalance().getCurrency())
                                    .build());
                            return bankAccountRepository.save(account)
                                    .flatMap(savedAccount -> postTransaction(transaction));
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
                                    BigDecimal originalBalanceSource = account.getAccountBalance().getBalance();
                                    BigDecimal originalBalanceDestination = accountDestination.getAccountBalance().getBalance();
                                    TransactionRequest transactionWithdrawal = createTransaction(account,
                                            transfer.getDestinationProductId(),
                                            WITHDRAWAL, originalBalanceSource.subtract(BigDecimal.valueOf(transfer.getAmount())),
                                            BigDecimal.valueOf(transfer.getAmount()));
                                    TransactionRequest transactionDeposit = createTransaction(accountDestination,
                                            account.getId(), DEPOSIT, originalBalanceDestination
                                                    .add(BigDecimal.valueOf(transfer.getAmount())),
                                            BigDecimal.valueOf(transfer.getAmount()));
                                    updateAccountBalance(account, BigDecimal.valueOf(transactionWithdrawal.getBalance()));
                                    updateAccountBalance(accountDestination, BigDecimal.valueOf(transactionDeposit.getBalance()));
                                    return bankAccountRepository.saveAll(Flux.just(account, accountDestination))
                                            .collectList()
                                            .flatMap(savedAccounts ->
                                                    Flux.zip(postTransaction(transactionWithdrawal),
                                                                    postTransaction(transactionDeposit))
                                                            .next()
                                                            .map(results ->
                                                                    new TransactionResponse(results.getT1().getId(), results.getT1().getProductId(),
                                                                            results.getT2().getProductId(), results.getT1().getType(),
                                                                            results.getT1().getAmount(), results.getT1().getClientId()))
                                            )
                                            .onErrorResume(e -> {
                                                updateAccountBalance(account, originalBalanceSource);
                                                updateAccountBalance(accountDestination, originalBalanceDestination);
                                                return bankAccountRepository.saveAll(Flux.just(account, accountDestination))
                                                        .then(Mono.error(new CustomException(CustomError.E_OPERATION_FAILED)));
                                            });
                                })
                        )
                );
    }

    private Mono<TransactionResponse> postTransaction(TransactionRequest transactionRequest) {
        return webClient.post(transactionServiceUrl.concat("/v1/transactions"), transactionRequest, TransactionResponse.class);
    }

    private void updateAccountBalance(BankAccount account, BigDecimal updatedBalance) {
        account.setAccountBalance(Balance.builder()
                .balance(updatedBalance)
                .currency(account.getAccountBalance().getCurrency())
                .build());
    }

    private BigDecimal calculateCommission(BankAccount account) {
        BigDecimal commissionFee = account.getCommissionFee() != null
                ? account.getCommissionFee()
                : BigDecimal.ZERO;
        return switch (AccountRequest.TypeEnum.valueOf(account.getType())) {
            case SAVINGS -> account.getMovementsUsed() >= account.getMovementLimit()
                    ? commissionFee
                    : BigDecimal.ZERO;
            case CHECKING -> account.getMaintenanceFee();
            case FIXED_TERM -> BigDecimal.ZERO;
        };
    }

    private TransactionRequest createTransaction(BankAccount account, String destinationProductId, TransactionRequest.TypeEnum type,
                                                 BigDecimal updatedBalance, BigDecimal amount) {
        return TransactionExternalMapper.INSTANCE.map(account, destinationProductId, amount, type, updatedBalance);
    }

}
