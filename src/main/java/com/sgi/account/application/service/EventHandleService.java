package com.sgi.account.application.service;

/**
 * Interface that defines services to handle events related to bank accounts.
 */
public interface EventHandleService {
    void validateExistBankAccount(String bootcoinId, String accountId);
}
