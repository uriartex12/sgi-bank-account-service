package com.sgi.account.infrastructure.subscriber.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Event class representing the existence of a bank account in the validation process.
 * This event carries information about whether the account exists or not, along with
 * the bootcoinId and accountId related to the validation process.
 * It is used for communication within the event-driven system.
 */
@Data
@Builder
@AllArgsConstructor
public class BankAccountExistEvent {
    private String bootcoinId;
    private String accountId;
    private Boolean exist;

    public static final String TOPIC = "validation-account-response";
}
