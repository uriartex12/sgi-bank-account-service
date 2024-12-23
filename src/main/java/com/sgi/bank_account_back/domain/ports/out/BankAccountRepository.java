package com.sgi.bank_account_back.domain.ports.out;
import com.sgi.bank_account_back.infrastructure.dto.*;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BankAccountRepository {
    Mono<AccountResponse> createAccount(Mono<AccountRequest> customer);
    Mono<Void> deleteAccount(String id);
    Flux<AccountResponse> getAllAccounts();
    Mono<AccountResponse> getAccountById(String id);
    Mono<AccountResponse> updateAccount(String id, Mono<AccountRequest> customer);
    Mono<TransactionResponse> depositToAccount(String idAccount, Mono<TransactionRequest> transactionRequest);
    Mono<TransactionResponse> withdrawFromAccount(String idAccount, Mono<TransactionRequest> transactionRequest);
    Mono<BalanceResponse> getClientBalances(String idAccount);
    Flux<TransactionResponse> getClientTransactions(String idAccount);
}
