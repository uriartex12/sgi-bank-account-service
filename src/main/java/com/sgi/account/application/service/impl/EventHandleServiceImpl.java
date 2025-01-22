package com.sgi.account.application.service.impl;

import com.sgi.account.application.service.EventHandleService;
import com.sgi.account.domain.ports.in.BankAccountService;
import com.sgi.account.infrastructure.dto.AccountResponse;
import com.sgi.account.infrastructure.subscriber.events.BankAccountExistEvent;
import com.sgi.account.infrastructure.subscriber.message.EventSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Service responsible for handling bank account validation and event sending.
 */
@Service
@RequiredArgsConstructor
public class EventHandleServiceImpl implements EventHandleService {

    private final BankAccountService bankAccountService;

    private final EventSender kafkaTemplate;

    /**
     * Validates whether the bank account exists and its type is valid (Checking or Savings).
     * If the account is valid, sends a validation event to Kafka.
     *
     * @param accountId The ID of the bank account to validate.
     */
    @Override
    public void validateExistBankAccount(String bootcoinId, String accountId) {
            bankAccountService.getAccountById(accountId)
                .flatMap(account -> {
                    boolean isValid = account.getType().equals(AccountResponse.TypeEnum.CHECKING)
                            || account.getType().equals(AccountResponse.TypeEnum.SAVINGS);
                    kafkaTemplate.sendEvent(BankAccountExistEvent.TOPIC,
                            BankAccountExistEvent.builder()
                                    .accountId(accountId)
                                    .bootcoinId(bootcoinId)
                                    .exist(isValid)
                                    .build());
                    return Mono.empty();
                })
                .switchIfEmpty(Mono.defer(Mono::empty)).subscribe();

    }

}
