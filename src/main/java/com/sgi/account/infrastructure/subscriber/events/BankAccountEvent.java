package com.sgi.account.infrastructure.subscriber.events;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * Event class representing a bank account event for a wallet validation process.
 * This event contains the bootcoinId and accountId that are part of the validation process.
 * It is used for communication within the event-driven system.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankAccountEvent {
    private String bootcoinId;
    private String accountId;

    public static final String TOPIC = "validation-exists-wallet";
}
