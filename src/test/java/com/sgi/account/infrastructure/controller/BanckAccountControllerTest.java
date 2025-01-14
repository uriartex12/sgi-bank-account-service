package com.sgi.account.infrastructure.controller;

import com.sgi.account.domain.ports.in.BankAccountService;
import com.sgi.account.domain.ports.in.TransactionService;
import com.sgi.account.helper.FactoryTest;
import com.sgi.account.infrastructure.dto.AccountRequest;
import com.sgi.account.infrastructure.dto.AccountResponse;
import com.sgi.account.infrastructure.dto.TransactionResponse;
import com.sgi.account.infrastructure.dto.TransferRequest;
import com.sgi.account.infrastructure.dto.BalanceResponse;
import com.sgi.account.infrastructure.dto.DepositRequest;
import com.sgi.account.infrastructure.dto.WithdrawalRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

/**
 * Test class for the {@link BanckAccountController}.
 * Utilizes {@code @WebFluxTest} to test the controller layer in isolation
 * without starting the full application context.
 */
@WebFluxTest(controllers = BanckAccountController.class)
public class BanckAccountControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private BankAccountService bankAccountService;

    @MockBean
    private TransactionService transactionService;

        @Test
        void createAccount_shouldReturnCreatedResponse() {
            AccountResponse accountResponse = FactoryTest.toFactoryBankAccount(AccountResponse.class);
            Mockito.when(bankAccountService.createAccount(any(Mono.class)))
                    .thenReturn(Mono.just(accountResponse));
             webTestClient.post()
                    .uri("/v1/accounts")
                    .bodyValue(FactoryTest.toFactoryBankAccount(AccountRequest.class))
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody(AccountResponse.class)
                    .consumeWith(accountResponseEntityExchangeResult -> {
                        AccountResponse actual = accountResponseEntityExchangeResult.getResponseBody();
                        Assertions.assertNotNull(Objects.requireNonNull(actual).getId());
                        Assertions.assertNotNull(actual.getAccountNumber());
                        Assertions.assertNull(actual.getCreatedDate());
                        Assertions.assertEquals(AccountResponse.TypeEnum.CHECKING, actual.getType());
                    })
                    .returnResult();
            Mockito.verify(bankAccountService, times(1)).createAccount(any(Mono.class));
        }

    @Test
    void deleteAccount_shouldReturnOkResponse() {
        String accountId = randomUUID().toString();
        Mockito.when(bankAccountService.deleteAccount(accountId)).thenReturn(Mono.empty());
        webTestClient.delete()
                .uri("/v1/accounts/{id}", accountId)
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    void getAccountById_shouldReturnAccountResponse() {
        AccountResponse accountResponse = FactoryTest.toFactoryBankAccount(AccountResponse.class);
        Mockito.when(bankAccountService.getAccountById(accountResponse.getId()))
                .thenReturn(Mono.just(accountResponse));
        webTestClient.get()
                .uri("/v1/accounts/{id}", accountResponse.getId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(AccountResponse.class)
                .consumeWith(System.out::println)
                .value(actual -> {
                    Assertions.assertEquals(accountResponse.getId(), actual.getId());
                    Assertions.assertEquals(accountResponse.getClientId(), actual.getClientId());
                });
    }

    @Test
    void getAllAccounts_shouldReturnFluxOfAccountResponse() {
        String clientId = UUID.randomUUID().toString();
        String type = "CHECKING";
        String accountId = UUID.randomUUID().toString();
        List<AccountResponse> accounts =  FactoryTest.toFactoryListBankAccounts();
        Flux<AccountResponse> accountsFlux = Flux.fromIterable(accounts);
        Mockito.when(bankAccountService.getAllAccounts(clientId, type, accountId)).thenReturn(accountsFlux);
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/accounts")
                        .queryParam("clientId", clientId)
                        .queryParam("type", type)
                        .queryParam("accountId", accountId)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(AccountResponse.class)
                .consumeWith(response -> {
                    List<AccountResponse> list = response.getResponseBody();
                    assertThat(list).isNotNull();
                    assertThat(list).hasSize(2);
                });
    }

    @Test
    void getClientBalances_shouldReturnBalanceResponse() {
        String idAccount = randomUUID().toString();
        BalanceResponse balanceResponse = FactoryTest.toFactoryBalanceClient();
        Mockito.when(bankAccountService.getClientBalances(idAccount)).thenReturn(Mono.just(balanceResponse));
        webTestClient.get()
                .uri("/v1/accounts/{accountId}/balances", idAccount)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(BalanceResponse.class)
                .isEqualTo(balanceResponse);
        Mockito.verify(bankAccountService, times(1)).getClientBalances(idAccount);
    }

    @Test
    void depositToAccount_shouldReturnTransactionResponse() {
        String accountId = randomUUID().toString();
        DepositRequest depositRequest = FactoryTest.toFactoryDepositRequest();
        TransactionResponse transactionResponse = FactoryTest.toFactoryTransactionResponse(accountId);

        Mockito.when(transactionService.depositToAccount(eq(accountId), any(Mono.class)))
                .thenReturn(Mono.just(transactionResponse));

        webTestClient.post()
                .uri("/v1/accounts/{accountId}/deposit", accountId)
                .bodyValue(depositRequest)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(TransactionResponse.class)
                .isEqualTo(transactionResponse);

        Mockito.verify(transactionService, times(1)).depositToAccount(eq(accountId), any(Mono.class));
    }

    @Test
    void transferToAccount_shouldReturnTransactionResponse() {
        String accountId = randomUUID().toString();
        TransferRequest transferRequest = FactoryTest.toFactoryTransferRequest(null);
        TransactionResponse transactionResponse = FactoryTest.toFactoryTransactionResponse(accountId);

        Mockito.when(transactionService.transferFunds(eq(accountId), any(Mono.class)))
                .thenReturn(Mono.just(transactionResponse));

        webTestClient.post()
                .uri("/v1/accounts/{accountId}/transfer", accountId)
                .bodyValue(transferRequest)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(TransactionResponse.class)
                .isEqualTo(transactionResponse);

        Mockito.verify(transactionService, times(1)).transferFunds(eq(accountId), any(Mono.class));
    }

    @Test
    void withdrawFromAccount_shouldReturnTransactionResponse() {
        String accountId = randomUUID().toString();
        WithdrawalRequest withdrawalRequest = FactoryTest.toFactoryWithdrawalRequest();
        TransactionResponse transactionResponse = FactoryTest.toFactoryTransactionResponse(accountId);

        Mockito.when(transactionService.withdrawFromAccount(eq(accountId), any(Mono.class)))
                .thenReturn(Mono.just(transactionResponse));

        webTestClient.post()
                .uri("/v1/accounts/{accountId}/withdrawal", accountId)
                .bodyValue(withdrawalRequest)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(TransactionResponse.class)
                .isEqualTo(transactionResponse);

        Mockito.verify(transactionService, times(1)).withdrawFromAccount(eq(accountId), any(Mono.class));
    }

    @Test
    void getClientTransactions_shouldReturnTransactionResponse() {
        String accountId = randomUUID().toString();
        List<TransactionResponse> transactionResponse = FactoryTest.toFactoryListTransactionResponse(accountId);
        Mockito.when(transactionService.getAccountIdTransactions(eq(accountId)))
                .thenReturn(Flux.fromIterable(transactionResponse));

        webTestClient.get()
                .uri("/v1/accounts/{accountId}/transactions", accountId)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(TransactionResponse.class)
                .isEqualTo(transactionResponse);

        Mockito.verify(transactionService, times(1)).getAccountIdTransactions(eq(accountId));
    }

    @Test
    void updateAccount_shouldReturnAccountResponse() {
        String accountId = randomUUID().toString();
        AccountRequest accountRequest = FactoryTest.toFactoryBankAccount(AccountRequest.class);
        AccountResponse accountResponse = FactoryTest.toFactoryBankAccount(AccountResponse.class);
        accountResponse.setId(accountId);

        Mockito.when(bankAccountService.updateAccount(eq(accountId), any(Mono.class)))
                .thenReturn(Mono.just(accountResponse));

        webTestClient.put()
                .uri("/v1/accounts/{accountId}", accountId)
                .bodyValue(accountRequest)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(AccountResponse.class);

        Mockito.verify(bankAccountService, times(1)).updateAccount(eq(accountId), any(Mono.class));
    }
}
