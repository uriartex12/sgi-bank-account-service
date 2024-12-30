package com.sgi.account.domain.ports.in;

import com.sgi.account.infrastructure.dto.AccountRequest;
import com.sgi.account.infrastructure.dto.AccountResponse;
import com.sgi.account.infrastructure.dto.BalanceResponse;
import com.sgi.account.infrastructure.dto.TransactionResponse;
import com.sgi.account.infrastructure.dto.DepositRequest;
import com.sgi.account.infrastructure.dto.WithdrawalRequest;
import com.sgi.account.infrastructure.dto.TransferRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service interface for managing bank accounts.
 * Defines the operations for creating, updating, deleting, and querying bank accounts and transactions.
 */
public interface BankAccountService {
    Mono<AccountResponse> createAccount(Mono<AccountRequest> customer);
    Mono<Void> deleteAccount(String id);
    Flux<AccountResponse> getAllAccounts();
    Mono<AccountResponse> getAccountById(String id);
    Mono<AccountResponse> updateAccount(String id, Mono<AccountRequest> account);
    Mono<TransactionResponse> depositToAccount(String idAccount, Mono<DepositRequest> depositRequestMono);
    Mono<TransactionResponse> withdrawFromAccount(String idAccount, Mono<WithdrawalRequest> withdrawalRequestMono);
    Mono<BalanceResponse> getClientBalances(String idAccount);
    Flux<TransactionResponse> getClientTransactions(String idAccount);
    Mono<TransactionResponse> transferFunds(String idAccount, Mono<TransferRequest> transferRequest);
}
