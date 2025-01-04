package com.sgi.account.application.service;

import com.sgi.account.application.service.impl.ValidationBankAccountImpl;
import com.sgi.account.domain.ports.out.BankAccountRepository;
import com.sgi.account.domain.ports.out.FeignExternalService;
import com.sgi.account.domain.shared.CustomError;
import com.sgi.account.helper.FactoryTest;
import com.sgi.account.infrastructure.dto.AccountRequest;
import com.sgi.account.infrastructure.dto.Credit;
import com.sgi.account.infrastructure.dto.Customer;
import com.sgi.account.infrastructure.exception.CustomException;
import org.junit.jupiter.api.Assertions;
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
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

/**
 * Unit tests for ValidationBankAccountImpl, ensuring bank account validation logic.
 * Uses Mockito for mocking and JUnit 5 for testing.
 */
@ExtendWith(MockitoExtension.class)
public class ValidationBankAccountImplTest {

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private FeignExternalService webClient;

    @InjectMocks
    private ValidationBankAccountImpl validationBankAccount;

    private static final String creditServiceUrl = "localhost:8081/";

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(validationBankAccount, "creditServiceUrl", creditServiceUrl);
    }

    @Test
    void savingsAccount_shouldReturnAccountRequest() {
        AccountRequest accountRequest = FactoryTest.toFactoryBankAccount(AccountRequest.class);
        accountRequest.setType(AccountRequest.TypeEnum.SAVINGS);
        Customer customer = FactoryTest.toFactoryCustomerResponse();
        Credit credit = FactoryTest.toFactoryCreditResponse();

        when(webClient.getFlux(anyString(), anyString(), eq(Credit.class)))
                .thenReturn(Flux.just(credit));
        Mono<AccountRequest> result = validationBankAccount.savingsAccount(accountRequest, customer);

        StepVerifier.create(result)
                .expectNext(accountRequest)
                .verifyComplete();
    }

    @Test
    void savingsAccountNotVip_shouldReturnAccountRequest() {
        Customer customer = FactoryTest.toFactoryCustomerResponse();
        customer.setProfile(null);
        AccountRequest accountRequest = FactoryTest.toFactoryBankAccount(AccountRequest.class);
        accountRequest.setType(AccountRequest.TypeEnum.SAVINGS);
        accountRequest.setClientId(customer.getId());
        when(bankAccountRepository.existsByClientIdAndType(
                customer.getId(),
                accountRequest.getType().name()
        )).thenReturn(Mono.just(false));

        Mono<AccountRequest> result = validationBankAccount.savingsAccount(accountRequest, customer);
        StepVerifier.create(result)
                .assertNext(account -> {
                    assertNotNull(account);
                    assertEquals(accountRequest.getClientId(), account.getClientId());
                })
                .verifyComplete();
        verify(bankAccountRepository, times(1))
                .existsByClientIdAndType(customer.getId(), accountRequest.getType().name());
    }

    @Test
    void savingsAccountToTypeBusiness_shouldReturnCustomException() {
        Customer customer = FactoryTest.toFactoryCustomerResponse();
        customer.setType(Customer.TypeEnum.BUSINESS);
        AccountRequest accountRequest = FactoryTest.toFactoryBankAccount(AccountRequest.class);
        accountRequest.setType(AccountRequest.TypeEnum.SAVINGS);
        accountRequest.setClientId(customer.getId());

        Mono<AccountRequest> result = validationBankAccount.savingsAccount(accountRequest, customer);
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof CustomException
                                &&
                                "Business clients cannot have savings accounts.".equals(throwable.getMessage())
                ).verify();
    }

    @Test
    void savingsAccountNotVipExist_shouldReturnCustomException() {
        Customer customer = FactoryTest.toFactoryCustomerResponse();
        customer.setProfile(null);
        AccountRequest accountRequest = FactoryTest.toFactoryBankAccount(AccountRequest.class);
        accountRequest.setClientId(customer.getId());
        when(bankAccountRepository.existsByClientIdAndType(
                customer.getId(),
                accountRequest.getType().name()
        )).thenReturn(Mono.just(true));

        Mono<AccountRequest> result = validationBankAccount.savingsAccount(accountRequest, customer);
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof CustomException
                                &&
                                "ACCOUNT-007".equals(((CustomException) throwable).getCode())
                ).verify();
        verify(bankAccountRepository, times(1))
                .existsByClientIdAndType(customer.getId(), accountRequest.getType().name());
    }

    @Test
    void checkingAccountToTypeBusiness_shouldReturnAccountRequest() {
        Customer customer = FactoryTest.toFactoryCustomerResponse();
        customer.setType(Customer.TypeEnum.BUSINESS);
        customer.setProfile(Customer.ProfileEnum.PYME);
        Credit credit = FactoryTest.toFactoryCreditResponse();
        AccountRequest accountRequest = FactoryTest.toFactoryBankAccount(AccountRequest.class);
        when(webClient.getFlux(anyString(), anyString(), eq(Credit.class)))
                .thenReturn(Flux.just(credit));
        Mono<AccountRequest> result = validationBankAccount.checkingAccount(accountRequest, customer);

        StepVerifier.create(result)
                .expectNext(accountRequest)
                .verifyComplete();
    }

    @Test
    void checkingAccountToTypePerson_shouldReturnAccountRequest() {
        Customer customer = FactoryTest.toFactoryCustomerResponse();
        customer.setType(Customer.TypeEnum.PERSONAL);
        customer.setProfile(null);
        AccountRequest accountRequest = FactoryTest.toFactoryBankAccount(AccountRequest.class);
        accountRequest.setClientId(customer.getId());
        when(bankAccountRepository.existsByClientIdAndType(
                customer.getId(),
                accountRequest.getType().name()
        )).thenReturn(Mono.just(false));
        Mono<AccountRequest> result = validationBankAccount.checkingAccount(accountRequest, customer);
        StepVerifier.create(result)
                .assertNext(account -> {
                    assertNotNull(account);
                    assertEquals(accountRequest.getClientId(), account.getClientId());
                })
                .verifyComplete();
        verify(bankAccountRepository, times(1))
                .existsByClientIdAndType(customer.getId(), accountRequest.getType().name());
    }

    @Test
    void checkingAccountToPerson_shouldReturnCustomException() {

        Customer customer = FactoryTest.toFactoryCustomerResponse();
        customer.setType(Customer.TypeEnum.PERSONAL);
        customer.setProfile(null);
        AccountRequest accountRequest = FactoryTest.toFactoryBankAccount(AccountRequest.class);
        accountRequest.setClientId(customer.getId());
        accountRequest.setMaintenanceFee(null);

        Mono<AccountRequest> result = validationBankAccount.checkingAccount(accountRequest, customer);

        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                    throwable instanceof CustomException
                            &&
                            "ACCOUNT-006".equals(((CustomException) throwable).getCode())
                )
                .verify();
    }

    @Test
    void checkingAccountToTypePerson_shouldThrowExceptionWhenAccountAlreadyExists() {
        Customer customer = FactoryTest.toFactoryCustomerResponse();
        customer.setType(Customer.TypeEnum.PERSONAL);
        customer.setProfile(null);
        AccountRequest accountRequest = FactoryTest.toFactoryBankAccount(AccountRequest.class);
        accountRequest.setClientId(customer.getId());
        when(bankAccountRepository.existsByClientIdAndType(
                customer.getId(),
                accountRequest.getType().name()
        )).thenReturn(Mono.just(true));
        Mono<AccountRequest> result = validationBankAccount.checkingAccount(accountRequest, customer);
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof CustomException
                                &&
                                "ACCOUNT-007".equals(((CustomException) throwable).getCode())
                )
                .verify();
    }

    @Test
    void fixedTermAccount_BusinessClient_ShouldThrowException() {
        Customer customer = FactoryTest.toFactoryCustomerResponse();
        customer.setType(Customer.TypeEnum.BUSINESS);
        AccountRequest accountRequest = FactoryTest.toFactoryBankAccount(AccountRequest.class);
        Mono<AccountRequest> result = validationBankAccount.fixedTermAccount(accountRequest, customer);

        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof CustomException
                                &&
                                "ACCOUNT-012".equals(((CustomException) throwable).getCode())
                )
                .verify();
    }

    @Test
    void fixedTermAccount_MissingTransactionDay_ShouldThrowException() {
        Customer customer = FactoryTest.toFactoryCustomerResponse();
        customer.setType(Customer.TypeEnum.PERSONAL);
        AccountRequest accountRequest = FactoryTest.toFactoryBankAccount(AccountRequest.class);
        accountRequest.setTransactionDay(null);

        Mono<AccountRequest> result = validationBankAccount.fixedTermAccount(accountRequest, customer);

        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof CustomException
                                &&
                                "ACCOUNT-006".equals(((CustomException) throwable).getCode())
                )
                .verify();
    }

    @Test
    void fixedTermAccount_ExistingAccount_ShouldThrowException() {
        Customer customer = FactoryTest.toFactoryCustomerResponse();
        customer.setType(Customer.TypeEnum.PERSONAL); // Cliente tipo persona
        AccountRequest accountRequest = FactoryTest.toFactoryBankAccount(AccountRequest.class);
        accountRequest.setTransactionDay(LocalDate.now());

        when(bankAccountRepository.existsByClientIdAndType(
                accountRequest.getClientId(),
                accountRequest.getType().name()
        )).thenReturn(Mono.just(true));

        Mono<AccountRequest> result = validationBankAccount.fixedTermAccount(accountRequest, customer);

        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof CustomException
                                &&
                                "ACCOUNT-007".equals(((CustomException) throwable).getCode())
                )
                .verify();

        verify(bankAccountRepository, times(1))
                .existsByClientIdAndType(accountRequest.getClientId(), accountRequest.getType().name());
    }

    @Test
    void fixedTermAccount_ValidPersonalClient_ShouldReturnAccount() {
        Customer customer = FactoryTest.toFactoryCustomerResponse();
        customer.setType(Customer.TypeEnum.PERSONAL);
        AccountRequest accountRequest = FactoryTest.toFactoryBankAccount(AccountRequest.class);
        accountRequest.setTransactionDay(LocalDate.now());

        when(bankAccountRepository.existsByClientIdAndType(
                accountRequest.getClientId(),
                accountRequest.getType().name()
        )).thenReturn(Mono.just(false));

        Mono<AccountRequest> result = validationBankAccount.fixedTermAccount(accountRequest, customer);

        StepVerifier.create(result)
                .assertNext(account -> {
                    assertNotNull(account);
                    assertEquals(BigDecimal.ZERO, account.getMaintenanceFee());
                    assertEquals(1, account.getMovementLimit());
                })
                .verifyComplete();

        verify(bankAccountRepository, times(1))
                .existsByClientIdAndType(accountRequest.getClientId(), accountRequest.getType().name());
    }

    @Test
    void fixedTermAccount_OtherClientType_ShouldReturnAccountDirectly() {
        Customer customer = FactoryTest.toFactoryCustomerResponse();
        customer.setType(null);
        AccountRequest accountRequest = FactoryTest.toFactoryBankAccount(AccountRequest.class);

        Mono<AccountRequest> result = validationBankAccount.fixedTermAccount(accountRequest, customer);

        StepVerifier.create(result)
                .assertNext(Assertions::assertNotNull)
                .verifyComplete();
    }


}
