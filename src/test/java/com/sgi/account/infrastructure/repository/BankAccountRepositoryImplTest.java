package com.sgi.account.infrastructure.repository;

import com.sgi.account.domain.model.BankAccount;
import com.sgi.account.helper.FactoryTest;
import com.sgi.account.infrastructure.dto.AccountResponse;
import com.sgi.account.infrastructure.mapper.BankAccountMapper;
import com.sgi.account.infrastructure.repository.impl.BankAccountRepositoryImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the BankAccountRepositoryImpl class.
 * This class contains test cases that verify the functionality of the
 * BankAccountRepositoryImpl implementation. It uses mocks to test the
 * repository methods in isolation.
 */
@ExtendWith(MockitoExtension.class)
public class BankAccountRepositoryImplTest {

    @InjectMocks
    private BankAccountRepositoryImpl bankAccountRepository;

    @Mock
    private BankAccountRepositoryJpa repositoryJpa;

    @Test
    public void testSave() {
        BankAccount bankAccount = FactoryTest.toFactoryEntityBankAccount();
        AccountResponse accountResponse = BankAccountMapper.INSTANCE.toAccountResponse(bankAccount);
        when(repositoryJpa.save(bankAccount)).thenReturn(Mono.just(bankAccount));
        Mono<AccountResponse> result = bankAccountRepository.save(bankAccount);
        StepVerifier.create(result)
                .expectNext(accountResponse)
                .verifyComplete();

        verify(repositoryJpa, times(1)).save(bankAccount);
    }

    @Test
    public void testSaveAll() {
        BankAccount bankAccount1 = FactoryTest.toFactoryEntityBankAccount();
        BankAccount bankAccount2 = FactoryTest.toFactoryEntityBankAccount();
        AccountResponse accountResponse1 = BankAccountMapper.INSTANCE.toAccountResponse(bankAccount1);
        AccountResponse accountResponse2 =  BankAccountMapper.INSTANCE.toAccountResponse(bankAccount2);
        when(repositoryJpa.saveAll(any(Flux.class))).thenReturn(Flux.just(bankAccount1, bankAccount2));
        Flux<AccountResponse> result = bankAccountRepository.saveAll(Flux.just(bankAccount1, bankAccount2));
        StepVerifier.create(result)
                .expectNext(accountResponse1, accountResponse2)
                .verifyComplete();

        verify(repositoryJpa, times(1)).saveAll(any(Flux.class));
    }

    @Test
    public void testFindById() {
        String accountId = UUID.randomUUID().toString();
        BankAccount bankAccount =  FactoryTest.toFactoryEntityBankAccount();
        when(repositoryJpa.findById(accountId))
                .thenReturn(Mono.just(bankAccount));
        Mono<BankAccount> result = bankAccountRepository.findById(accountId);
        StepVerifier.create(result)
                .expectNext(bankAccount)
                .verifyComplete();

        verify(repositoryJpa, times(1)).findById(accountId);
    }

    @Test
    public void testFindAll() {
        BankAccount bankAccount1 = FactoryTest.toFactoryEntityBankAccount();
        BankAccount bankAccount2 = FactoryTest.toFactoryEntityBankAccount();
        when(repositoryJpa.findAllByClientIdOrTypeOrId(anyString(), anyString(), anyString()))
                .thenReturn(Flux.just(bankAccount1, bankAccount2));
        Flux<AccountResponse> result = bankAccountRepository.findAll(anyString(), anyString(), anyString());
        result.collectList().subscribe(responses -> {
            assertNotNull(responses);
            assertEquals(2, responses.size());
        });

        verify(repositoryJpa, times(1))
                .findAllByClientIdOrTypeOrId(anyString(), anyString(), anyString());
    }

    @Test
    public void testDelete() {
        BankAccount bankAccount = FactoryTest.toFactoryEntityBankAccount();
        bankAccount.setId(UUID.randomUUID().toString());
        when(repositoryJpa.delete(bankAccount)).thenReturn(Mono.empty());
        Mono<Void> result = bankAccountRepository.delete(bankAccount);
        StepVerifier.create(result)
                .verifyComplete();
        verify(repositoryJpa, times(1)).delete(bankAccount);
    }

    @Test
    public void testExistsByClientIdAndType() {
        String clientId = UUID.randomUUID().toString();
        when(repositoryJpa.existsByClientIdAndType(clientId, "SAVINGS"))
                .thenReturn(Mono.just(true));

        Mono<Boolean> result = bankAccountRepository.existsByClientIdAndType(clientId, "SAVINGS");
        StepVerifier.create(result)
                .expectNext(true)
                .verifyComplete();

        verify(repositoryJpa, times(1)).existsByClientIdAndType(clientId, "SAVINGS");
    }

}
