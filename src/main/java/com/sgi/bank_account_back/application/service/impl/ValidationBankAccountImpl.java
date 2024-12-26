package com.sgi.bank_account_back.application.service.impl;

import com.sgi.bank_account_back.application.service.ValidationBankAccount;
import com.sgi.bank_account_back.domain.ports.out.BankAccountRepository;
import com.sgi.bank_account_back.infrastructure.dto.AccountRequest;
import com.sgi.bank_account_back.domain.shared.CustomError;
import com.sgi.bank_account_back.infrastructure.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import static com.sgi.bank_account_back.infrastructure.dto.CustomerResponse.TypeEnum.BUSINESS;
import static com.sgi.bank_account_back.infrastructure.dto.CustomerResponse.TypeEnum.PERSONAL;

@Service
@RequiredArgsConstructor
public class ValidationBankAccountImpl implements ValidationBankAccount {

    private final BankAccountRepository bankAccountRepository;

    @Override
    public Mono<AccountRequest> savingsAccount(AccountRequest account, String customerType) {
        if (BUSINESS.getValue().equals(customerType))
            return Mono.error(new CustomException(CustomError.E_BUSINESS_CLIENT_CANNOT_HAVE_SAVINGS));
        if (account.getMovementLimit() == null || account.getMovementLimit() <= 0)
            return Mono.error(new CustomException(CustomError.E_MISSING_REQUIRED_ACCOUNT_DATA));
        if (PERSONAL.getValue().equals(customerType)) {
            return findByClientIdAndType(account.getClientId(), account.getType().name())
                    .flatMap(exists -> {
                        if (exists)
                            return Mono.error(new CustomException(CustomError.E_MAX_SAVINGS_ACCOUNTS_REACHED));
                        account.setMaintenanceFee(BigDecimal.ZERO);
                        return Mono.just(account);
                    });
        }
        return Mono.just(account);
    }

    @Override
    public Mono<AccountRequest> checkingAccount(AccountRequest account, String customerType) {
        if (account.getMaintenanceFee() == null || account.getMaintenanceFee().compareTo(BigDecimal.ZERO) <= 0)
            return Mono.error(new CustomException(CustomError.E_MISSING_REQUIRED_ACCOUNT_DATA));
        if (PERSONAL.getValue().equals(customerType)) {
            return findByClientIdAndType(account.getClientId(), account.getType().name())
                    .flatMap(exists -> {
                        if (exists)
                            return Mono.error(new CustomException(CustomError.E_MAX_SAVINGS_ACCOUNTS_REACHED));
                        account.setMovementLimit(null);
                        return Mono.just(account);
                    });
        }
        account.setMovementLimit(null);
        return Mono.just(account);
    }

    @Override
    public Mono<AccountRequest> fixedTermAccount(AccountRequest account, String customerType) {
        if (customerType.equals(BUSINESS.getValue()))
            return Mono.error(new CustomException(CustomError.E_BUSINESS_CLIENT_CANNOT_HAVE_FIXED_TERM));
        if (account.getTransactionDay() == null)
            return Mono.error(new CustomException(CustomError.E_MISSING_REQUIRED_ACCOUNT_DATA));
        if (PERSONAL.getValue().equals(customerType)) {
            return findByClientIdAndType(account.getClientId(), account.getType().name())
                    .flatMap(exists -> {
                        if (exists)
                            return Mono.error(new CustomException(CustomError.E_MAX_SAVINGS_ACCOUNTS_REACHED));
                        account.setMaintenanceFee(BigDecimal.ZERO);
                        account.setMovementLimit(1);
                        return Mono.just(account);
                    });
        }

        return Mono.just(account);
    }

    public Mono<Boolean> findByClientIdAndType(String clientId, String type) {
        return bankAccountRepository.existsByClientIdAndType(clientId, type);
    }
}
