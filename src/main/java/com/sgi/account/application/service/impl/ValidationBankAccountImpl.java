package com.sgi.account.application.service.impl;

import com.sgi.account.application.service.ValidationBankAccount;
import com.sgi.account.domain.ports.out.BankAccountRepository;
import com.sgi.account.domain.ports.out.FeignExternalService;
import com.sgi.account.domain.shared.CustomError;
import com.sgi.account.infrastructure.dto.AccountRequest;
import com.sgi.account.infrastructure.dto.Credit;
import com.sgi.account.infrastructure.dto.Customer;
import com.sgi.account.infrastructure.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import java.util.function.Predicate;

import static com.sgi.account.infrastructure.dto.Customer.ProfileEnum.PYME;
import static com.sgi.account.infrastructure.dto.Customer.ProfileEnum.VIP;
import static com.sgi.account.infrastructure.dto.Customer.TypeEnum.BUSINESS;
import static com.sgi.account.infrastructure.dto.Customer.TypeEnum.PERSONAL;

/**
 * Implementation of the bank account validation service.
 */
@Service
@RequiredArgsConstructor
public class ValidationBankAccountImpl implements ValidationBankAccount {

    @Value("${feign.client.config.credit-service.url}")
    private String creditServiceUrl;

    private final BankAccountRepository bankAccountRepository;
    private final FeignExternalService webClient;
    Predicate<Customer> isPersonal = c -> PERSONAL.equals(c.getType());
    Predicate<Customer> isBusiness = c -> BUSINESS.equals(c.getType());
    Predicate<Customer> isVip = c -> VIP.equals(c.getProfile());
    Predicate<Customer> isPyme = c -> PYME.equals(c.getProfile());

    @Override
    public Mono<AccountRequest> savingsAccount(AccountRequest account, Customer customer) {
        if (isBusiness.test(customer)) {
            return Mono.error(new CustomException(CustomError.E_BUSINESS_CLIENT_CANNOT_HAVE_SAVINGS));
        }
        return validateAccountData(account)
                .then(handlePersonalAccount(account, customer));
    }

    @Override
    public Mono<AccountRequest> checkingAccount(AccountRequest account, Customer customer) {
        if (isBusiness.test(customer) && isPyme.test(customer)) {
            return checkCreditCardForClient(account, customer.getId());
        }
        if (isPersonal.test(customer) && (account.getMaintenanceFee() == null
                || account.getMaintenanceFee().compareTo(BigDecimal.ZERO) <= 0)) {
            return Mono.error(new CustomException(CustomError.E_MISSING_REQUIRED_ACCOUNT_DATA));
        }
        if (isPersonal.test(customer)) {
            return findByClientIdAndType(account.getClientId(), account.getType().name())
                    .flatMap(exists -> exists
                            ? Mono.error(new CustomException(CustomError.E_MAX_SAVINGS_ACCOUNTS_REACHED))
                            : Mono.just(account).doOnNext(a -> a.setMovementLimit(null)));
        }
        account.setMovementLimit(null);
        return Mono.just(account);
    }

    @Override
    public Mono<AccountRequest> fixedTermAccount(AccountRequest account, Customer customer) {
        if (isBusiness.test(customer)) {
            return Mono.error(new CustomException(CustomError.E_BUSINESS_CLIENT_CANNOT_HAVE_FIXED_TERM));
        }
        if (account.getTransactionDay() == null) {
            return Mono.error(new CustomException(CustomError.E_MISSING_REQUIRED_ACCOUNT_DATA));
        }
        if (isPersonal.test(customer)) {
            return findByClientIdAndType(account.getClientId(), account.getType().name())
                    .flatMap(exists -> exists
                            ? Mono.error(new CustomException(CustomError.E_MAX_SAVINGS_ACCOUNTS_REACHED))
                            : Mono.just(account)
                            .doOnNext(a -> {
                                account.setMaintenanceFee(BigDecimal.ZERO);
                                account.setMovementLimit(1);

                            }));
        }
        return Mono.just(account);
    }

    private Mono<AccountRequest> checkCreditCardForClient(AccountRequest account, String clientId) {
        return webClient.getFlux(creditServiceUrl.concat("v1/credits/{clientId}/card"), clientId, Credit.class)
                .collectList()
                .filter(creditResponses -> !creditResponses.isEmpty())
                .switchIfEmpty(Mono.error(new CustomException(CustomError.E_MISSING_CREDIT_CARD)))
                .then(Mono.just(account));
    }

    public Mono<Boolean> findByClientIdAndType(String clientId, String type) {
        return bankAccountRepository.existsByClientIdAndType(clientId, type);
    }

    private Mono<Void> validateAccountData(AccountRequest account) {
        if (account.getMovementLimit() == null || account.getMovementLimit() <= 0) {
            return Mono.error(new CustomException(CustomError.E_MISSING_REQUIRED_ACCOUNT_DATA));
        }
        return Mono.empty();
    }

    private Mono<AccountRequest> handlePersonalAccount(AccountRequest account, Customer customer) {
        if (isPersonal.test(customer)) {
            if (isVip.test(customer)) {
                return checkCreditCardForClient(account, customer.getId());
            }
            return handleNonVipPersonalAccount(account);
        }
        return Mono.just(account);
    }

    private Mono<AccountRequest> handleNonVipPersonalAccount(AccountRequest account) {
        return findByClientIdAndType(account.getClientId(), account.getType().name())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new CustomException(CustomError.E_MAX_SAVINGS_ACCOUNTS_REACHED));
                    } else {
                        return Mono.just(account)
                                .doOnNext(a -> a.setMaintenanceFee(BigDecimal.ZERO));
                    }
                });
    }

}
