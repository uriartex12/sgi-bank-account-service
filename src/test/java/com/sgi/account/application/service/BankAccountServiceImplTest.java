package com.sgi.account.application.service;

import com.sgi.account.application.service.impl.BankAccountServiceImpl;
import com.sgi.account.domain.model.BankAccount;
import com.sgi.account.domain.ports.out.BankAccountRepository;
import com.sgi.account.domain.ports.out.FeignExternalService;
import com.sgi.account.helper.FactoryTest;
import com.sgi.account.infrastructure.dto.AccountRequest;
import com.sgi.account.infrastructure.dto.AccountResponse;
import com.sgi.account.infrastructure.dto.Customer;
import com.sgi.account.infrastructure.dto.BalanceResponse;
import com.sgi.account.infrastructure.exception.CustomException;
import com.sgi.account.infrastructure.mapper.BankAccountMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit test class for the BankAccountServiceImpl class.
 * This class contains tests to validate the functionality of the BankAccountServiceImpl.
 * It uses Mockito for mocking dependencies and JUnit 5 for running tests.
 */
@ExtendWith(MockitoExtension.class)
public class BankAccountServiceImplTest {

    @Mock
    private BankAccountRepository bankAccountRepository;

    private static final String customerServiceUrl = "localhost:8081/";

    @Mock
    private FeignExternalService webClient;

    @Mock
    private ValidationBankAccount validateSavingsAccount;

    @InjectMocks
    private BankAccountServiceImpl bankAccountService;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(bankAccountService, "customerServiceUrl", customerServiceUrl);
    }

    @Test
    void createAccount_shouldReturnCreatedResponse() {
        Customer customer = FactoryTest.toFactoryCustomerResponse();
        AccountRequest accountRequest = FactoryTest.toFactoryBankAccount(AccountRequest.class);
        AccountResponse accountResponse =  FactoryTest.toFactoryBankAccount(AccountResponse.class);
        when(webClient.getMono(anyString(), anyString(), eq(Customer.class)))
                .thenReturn(Mono.just(customer));
        when(validateSavingsAccount.checkingAccount(any(), eq(customer)))
                .thenReturn(Mono.just(accountRequest));
        when(bankAccountRepository.save(any(BankAccount.class)))
                .thenReturn(Mono.just(accountResponse));
        Mono<AccountResponse> result = bankAccountService.createAccount(Mono.just(accountRequest));
        StepVerifier.create(result)
                .expectNext(accountResponse)
                .verifyComplete();

        verify(webClient, times(1)).getMono(anyString(), anyString(), eq(Customer.class));
        verify(bankAccountRepository, times(1)).save(any(BankAccount.class));
    }

    @Test
    void deleteAccount_shouldReturnVoid() {
        String accountId = UUID.randomUUID().toString();
        BankAccount bankAccount = FactoryTest.toFactoryEntityBankAccount();
        bankAccount.setId(accountId);
        when(bankAccountRepository.findById(accountId)).thenReturn(Mono.just(bankAccount));
        when(bankAccountRepository.delete(bankAccount)).thenReturn(Mono.empty());
        Mono<Void> result = bankAccountService.deleteAccount(accountId);
        StepVerifier.create(result)
                .verifyComplete();
        verify(bankAccountRepository).findById(accountId);
        verify(bankAccountRepository).delete(bankAccount);
    }

    @Test
    void deleteAccount_shouldReturnNotFound() {
        String accountId = UUID.randomUUID().toString();
        when(bankAccountRepository.findById(accountId)).thenReturn(Mono.empty());
        Mono<Void> result = bankAccountService.deleteAccount(accountId);
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> true)
                .verify();

        verify(bankAccountRepository).findById(accountId);
        verifyNoMoreInteractions(bankAccountRepository);
    }

    @Test
    void getAllAccounts_shouldReturnListAccountResponse() {
        List<AccountResponse> accounts = FactoryTest.toFactoryListBankAccounts();
        when(bankAccountRepository.findAll(anyString(), anyString(), anyString())).thenReturn(Flux.fromIterable(accounts));
        Flux<AccountResponse> result = bankAccountService.getAllAccounts(anyString(), anyString(), anyString());

        StepVerifier.create(result)
                .expectNextCount(2)
                .verifyComplete();
        verify(bankAccountRepository).findAll(anyString(), anyString(), anyString());
    }

    @Test
    void getAccountById_shouldReturnListAccountResponse() {
        String accountId = UUID.randomUUID().toString();
        BankAccount bankAccount = FactoryTest.toFactoryEntityBankAccount();
        bankAccount.setId(accountId);
        AccountResponse accountResponse = BankAccountMapper.INSTANCE.toAccountResponse(bankAccount);
        when(bankAccountRepository.findById(accountId)).thenReturn(Mono.just(bankAccount));
        Mono<AccountResponse> result = bankAccountService.getAccountById(accountId);
        StepVerifier.create(result)
                .expectNext(accountResponse)
                .verifyComplete();
        verify(bankAccountRepository).findById(accountId);
    }

    @Test
    void getAccountById_shouldReturnNotFound() {
        String accountId = UUID.randomUUID().toString();
        when(bankAccountRepository.findById(accountId)).thenReturn(Mono.empty());
        Mono<AccountResponse> result = bankAccountService.getAccountById(accountId);
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof CustomException
                                &&
                                "Bank account not found".equals(throwable.getMessage())
                )
                .verify();
        verify(bankAccountRepository).findById(accountId);
    }

    @Test
    void updateAccount_shouldReturnAccountResponse() {
        String accountId = UUID.randomUUID().toString();
        BankAccount bankAccount = FactoryTest.toFactoryEntityBankAccount();
        bankAccount.setId(accountId);
        AccountRequest accountRequest = FactoryTest.toFactoryBankAccount(AccountRequest.class);
        AccountResponse accountResponse = BankAccountMapper.INSTANCE.toAccountResponse(bankAccount);

        when(bankAccountRepository.findById(accountId)).thenReturn(Mono.just(bankAccount));
        when(bankAccountRepository.save(argThat(account -> account.getId().equals(accountId))))
                .thenReturn(Mono.just(accountResponse));
        Mono<AccountResponse> result = bankAccountService.updateAccount(accountId, Mono.just(accountRequest));
        StepVerifier.create(result)
                .expectNext(accountResponse)
                .verifyComplete();
        verify(bankAccountRepository).findById(accountId);
    }

    @Test
    void getClientBalance_shouldReturnBalanceResponse() {
        String accountId = UUID.randomUUID().toString();
        BankAccount bankAccount = FactoryTest.toFactoryEntityBankAccount();
        bankAccount.setId(accountId);
        BalanceResponse balanceResponse = BankAccountMapper.INSTANCE.toBalance(bankAccount);
        when(bankAccountRepository.findById(accountId)).thenReturn(Mono.just(bankAccount));

        Mono<BalanceResponse> result = bankAccountService.getClientBalances(accountId);
        StepVerifier.create(result)
                .expectNext(balanceResponse)
                .verifyComplete();
        verify(bankAccountRepository).findById(accountId);
    }

}
