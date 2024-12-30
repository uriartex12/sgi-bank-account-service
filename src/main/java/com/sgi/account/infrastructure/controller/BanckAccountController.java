package com.sgi.account.infrastructure.controller;

import com.sgi.account.domain.ports.in.BankAccountService;
import com.sgi.account.domain.ports.in.TransactionService;
import com.sgi.account.infrastructure.dto.AccountRequest;
import com.sgi.account.infrastructure.dto.AccountResponse;
import com.sgi.account.infrastructure.dto.DepositRequest;
import com.sgi.account.infrastructure.dto.TransactionResponse;
import com.sgi.account.infrastructure.dto.BalanceResponse;
import com.sgi.account.infrastructure.dto.TransferRequest;
import com.sgi.account.infrastructure.dto.WithdrawalRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Controller to handle operations related to credits.
 */
@RestController
@AllArgsConstructor
public class BanckAccountController implements V1Api {

    private final BankAccountService bankAccountService;

    private final TransactionService transactionService;

    @Override
    public Mono<ResponseEntity<AccountResponse>> createAccount(Mono<AccountRequest> accountRequest, ServerWebExchange exchange) {
        return bankAccountService.createAccount(accountRequest)
                .map(bankAccount -> ResponseEntity.status(HttpStatus.CREATED).body(bankAccount));
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteAccount(String idAccount, ServerWebExchange exchange) {
        return bankAccountService.deleteAccount(idAccount)
                .map(bankAccount -> ResponseEntity.ok().body(bankAccount));
    }

    @Override
    public Mono<ResponseEntity<TransactionResponse>> depositToAccount(
            String idAccount,
            Mono<DepositRequest> depositRequestMono,
            ServerWebExchange exchange) {
        return transactionService.depositToAccount(idAccount, depositRequestMono)
                .map(bankAccount -> ResponseEntity.ok().body(bankAccount));
    }

    @Override
    public Mono<ResponseEntity<AccountResponse>> getAccountById(String idAccount, ServerWebExchange exchange) {
        return bankAccountService.getAccountById(idAccount)
                .map(bankAccount -> ResponseEntity.ok().body(bankAccount));
    }

    @Override
    public Mono<ResponseEntity<Flux<AccountResponse>>> getAllAccounts(ServerWebExchange exchange) {
        return Mono.fromSupplier(() -> ResponseEntity.ok().body(bankAccountService.getAllAccounts()));
    }

    @Override
    public Mono<ResponseEntity<BalanceResponse>> getClientBalances(String idAccount, ServerWebExchange exchange) {
        return bankAccountService.getClientBalances(idAccount)
                .map(balance -> ResponseEntity.ok().body(balance));
    }

    @Override
    public Mono<ResponseEntity<Flux<TransactionResponse>>> getClientTransactions(
            String accountId,
            ServerWebExchange exchange) {
        return Mono.fromSupplier(() -> ResponseEntity.ok().body(transactionService.getClientTransactions(accountId)));
    }

    @Override
    public Mono<ResponseEntity<TransactionResponse>> transferToAccount(
            String accountId,
            Mono<TransferRequest> transferRequest,
            ServerWebExchange exchange) {
        return transactionService.transferFunds(accountId, transferRequest)
                .map(transactionResponse -> ResponseEntity.ok().body(transactionResponse));
    }

    @Override
    public Mono<ResponseEntity<AccountResponse>> updateAccount(
            String idAccount,
            Mono<AccountRequest> accountRequest,
            ServerWebExchange exchange) {
        return bankAccountService.updateAccount(idAccount, accountRequest)
                .map(accountResponse -> ResponseEntity.ok().body(accountResponse));
    }

    @Override
    public Mono<ResponseEntity<TransactionResponse>> withdrawFromAccount(
            String idAccount,
            Mono<WithdrawalRequest> withdrawalRequestMono,
            ServerWebExchange exchange) {
        return transactionService.withdrawFromAccount(idAccount, withdrawalRequestMono)
                .map(transactionResponse -> ResponseEntity.ok().body(transactionResponse));
    }
}
