package com.sgi.account.domain.ports.in;

import com.sgi.account.infrastructure.dto.DepositRequest;
import com.sgi.account.infrastructure.dto.TransactionResponse;
import com.sgi.account.infrastructure.dto.TransferRequest;
import com.sgi.account.infrastructure.dto.WithdrawalRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service interface for managing transactions.
 * Defines the operations for deposit, withdraw, transfer and transactions.
 */
public interface TransactionService {

    Flux<TransactionResponse> getAccountIdTransactions(String idAccount);
    Mono<TransactionResponse> depositToAccount(String idAccount, Mono<DepositRequest> depositRequestMono);
    Mono<TransactionResponse> transferFunds(String idAccount, Mono<TransferRequest> transferRequest);
    Mono<TransactionResponse> withdrawFromAccount(String idAccount, Mono<WithdrawalRequest> withdrawalRequestMono);

}
