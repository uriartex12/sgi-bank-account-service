package com.sgi.bank_account_back.domain.ports.in;

import com.sgi.bank_account_back.infrastructure.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface BankAccountService {
    Mono<ResponseEntity<AccountResponse>> createAccount(Mono<AccountRequest> customer);
    Mono<ResponseEntity<Void>> deleteAccount(String id);
    Mono<ResponseEntity<Flux<AccountResponse>>> getAllAccounts();
    Mono<ResponseEntity<AccountResponse>> getAccountById(String id);
    Mono<ResponseEntity<AccountResponse>> updateAccount(String id, Mono<AccountRequest> account);
    Mono<ResponseEntity<TransactionResponse>> depositToAccount(String idAccount, Mono<TransactionRequest> transactionRequest);
    Mono<ResponseEntity<TransactionResponse>> withdrawFromAccount(String idAccount, Mono<TransactionRequest> transactionRequest);
    Mono<ResponseEntity<BalanceResponse>> getClientBalances(String idAccount);
    Mono<ResponseEntity<Flux<TransactionResponse>>> getClientTransactions(String idAccount);
}
