package com.sgi.account.application.service;

import com.sgi.account.application.service.impl.TransactionServiceImpl;
import com.sgi.account.domain.model.Balance;
import com.sgi.account.domain.model.BankAccount;
import com.sgi.account.domain.ports.out.BankAccountRepository;
import com.sgi.account.domain.ports.out.FeignExternalService;
import com.sgi.account.helper.FactoryTest;
import com.sgi.account.infrastructure.dto.DepositRequest;
import com.sgi.account.infrastructure.dto.TransactionRequest;
import com.sgi.account.infrastructure.dto.TransferRequest;
import com.sgi.account.infrastructure.dto.TransactionResponse;
import com.sgi.account.infrastructure.dto.WithdrawalRequest;
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

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.any;

/**
 * Unit test class for the TransactionServiceImpl class.
 * This class contains tests to validate the functionality of the TransactionServiceImpl,
 * focusing on various transaction-related operations. It uses Mockito to mock dependencies
 * and JUnit 5 to run the tests.
 */
@ExtendWith(MockitoExtension.class)
public class TransactionServiceImplTest {

    @InjectMocks
    private TransactionServiceImpl transactionService;

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private FeignExternalService feignExternalService;

    private static final String transactionServiceUrl = "localhost:8081/";

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(transactionService, "transactionServiceUrl", transactionServiceUrl);
    }

    @Test
    void testGetAccountIdTransactions_Success() {
        BankAccount bankAccount = FactoryTest.toFactoryEntityBankAccount();
        TransactionResponse transactionResponse = FactoryTest.toFactoryToClientIdTransactionResponse(bankAccount.getId(), bankAccount.getClientId());

        when(bankAccountRepository.findById(bankAccount.getId())).thenReturn(Mono.just(bankAccount));
        when(feignExternalService.getFlux(anyString(), anyString(), eq(TransactionResponse.class)))
                .thenReturn(Flux.just(transactionResponse));

        Flux<TransactionResponse> result = transactionService.getAccountIdTransactions(bankAccount.getId());

        StepVerifier.create(result)
                .expectNext(transactionResponse)
                .verifyComplete();

        verify(bankAccountRepository).findById(bankAccount.getId());
        verify(feignExternalService).getFlux(anyString(), anyString(), eq(TransactionResponse.class));
    }

    @Test
    void testDepositToAccount_Success() {
        BankAccount bankAccount = FactoryTest.toFactoryEntityBankAccount();
        bankAccount.setMaintenanceFee(BigDecimal.ONE);
        TransactionResponse transactionResponse = FactoryTest.toFactoryToClientIdTransactionResponse(bankAccount.getId(), bankAccount.getClientId());
        DepositRequest depositRequest = FactoryTest.toFactoryDepositRequest();
        when(bankAccountRepository.findById(bankAccount.getId())).thenReturn(Mono.just(bankAccount));
        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(Mono.just(BankAccountMapper.INSTANCE.toAccountResponse(bankAccount)));
        when(feignExternalService.post(anyString(), any(TransactionRequest.class), eq(TransactionResponse.class)))
                .thenReturn(Mono.just(transactionResponse));

        Mono<TransactionResponse> result = transactionService.depositToAccount(bankAccount.getId(), Mono.just(depositRequest));

        StepVerifier.create(result)
                .expectNext(transactionResponse)
                .verifyComplete();
        verify(bankAccountRepository).findById(bankAccount.getId());
        verify(bankAccountRepository).save(any(BankAccount.class));
        verify(feignExternalService).post(anyString(), any(TransactionRequest.class), eq(TransactionResponse.class));
    }


    @Test
    void testWithdrawToAccount_Success() {
        BankAccount bankAccount = FactoryTest.toFactoryEntityBankAccount();
        bankAccount.setAccountBalance(new Balance(BigDecimal.valueOf(10000), "PEN"));
        bankAccount.setMaintenanceFee(BigDecimal.ONE);
        TransactionResponse transactionResponse = FactoryTest.toFactoryToClientIdTransactionResponse(bankAccount.getId(), bankAccount.getClientId());
        WithdrawalRequest withdrawalRequest = FactoryTest.toFactoryWithdrawalRequest();
        when(bankAccountRepository.findById(bankAccount.getId())).thenReturn(Mono.just(bankAccount));
        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(Mono.just(BankAccountMapper.INSTANCE.toAccountResponse(bankAccount)));
        when(feignExternalService.post(anyString(), any(TransactionRequest.class), eq(TransactionResponse.class)))
                .thenReturn(Mono.just(transactionResponse));

        Mono<TransactionResponse> result = transactionService.withdrawFromAccount(bankAccount.getId(), Mono.just(withdrawalRequest));

        StepVerifier.create(result)
                .expectNext(transactionResponse)
                .verifyComplete();
        verify(bankAccountRepository).findById(bankAccount.getId());
        verify(bankAccountRepository).save(any(BankAccount.class));
        verify(feignExternalService).post(anyString(), any(TransactionRequest.class), eq(TransactionResponse.class));
    }

    @Test
    void testWithdrawFromAccount_InsufficientBalance() {
        BankAccount mockAccount = FactoryTest.toFactoryEntityBankAccount();
        mockAccount.setAccountBalance(new Balance(BigDecimal.valueOf(100), "PEN"));
        mockAccount.setMaintenanceFee(BigDecimal.ONE);
        when(bankAccountRepository.findById(mockAccount.getId())).thenReturn(Mono.just(mockAccount));
        WithdrawalRequest withdrawalRequest = FactoryTest.toFactoryWithdrawalRequest();
        Mono<TransactionResponse> result = transactionService.withdrawFromAccount(mockAccount.getId(), Mono.just(withdrawalRequest));

        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof CustomException
                                &&
                                "Insufficient balance".equals(throwable.getMessage())
                )
                .verify();

        verify(bankAccountRepository).findById(mockAccount.getId());
        verify(bankAccountRepository, never()).save(any(BankAccount.class));
    }

    @Test
    void testTransferFunds_Success() {
        String sourceAccountId = UUID.randomUUID().toString();
        String destinationAccountId = UUID.randomUUID().toString();

        BankAccount sourceAccount = FactoryTest.toFactoryEntityBankAccount();
        sourceAccount.setId(sourceAccountId);
        sourceAccount.setAccountBalance(new Balance(BigDecimal.valueOf(100), "PEN"));

        BankAccount destinationAccount = FactoryTest.toFactoryEntityBankAccount();
        destinationAccount.setId(destinationAccountId);
        destinationAccount.setAccountBalance(new Balance(BigDecimal.valueOf(50), "PEN"));

        TransactionResponse transactionResponse = FactoryTest.toFactoryToClientIdTransactionResponse(sourceAccount.getId(), sourceAccount.getClientId());
        transactionResponse.setDestinationProductId(destinationAccount.getId());

        when(bankAccountRepository.findById(sourceAccountId)).thenReturn(Mono.just(sourceAccount));
        when(bankAccountRepository.findById(destinationAccountId)).thenReturn(Mono.just(destinationAccount));
        when(bankAccountRepository.saveAll(any(Flux.class))).thenReturn(Flux.just(sourceAccount, destinationAccount));
        when(feignExternalService.post(anyString(), any(TransactionRequest.class), eq(TransactionResponse.class)))
                .thenReturn(Mono.just(transactionResponse));
        TransferRequest transferRequest = FactoryTest.toFactoryTransferRequest(destinationAccountId);
        Mono<TransactionResponse> result = transactionService.transferFunds(sourceAccountId, Mono.just(transferRequest));

        StepVerifier.create(result)
                .expectNextCount(1)
                .verifyComplete();
        verify(bankAccountRepository).findById(sourceAccountId);
        verify(bankAccountRepository).findById(destinationAccountId);
        verify(bankAccountRepository).saveAll(any(Flux.class));
    }
}