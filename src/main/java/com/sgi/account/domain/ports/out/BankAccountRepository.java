package com.sgi.account.domain.ports.out;

import com.sgi.account.domain.model.BankAccount;
import com.sgi.account.infrastructure.dto.AccountResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository interface defining operations to manage credits.
 */
public interface BankAccountRepository {
    /**
     * Saves a Account in the repository.
     *
     * @param bankAccount the credit information to save
     * @return a Mono containing the saved account response
     */
    Mono<AccountResponse> save(BankAccount bankAccount);

    /**
     * Finds a account by its ID.
     *
     * @param id the unique identifier of the account
     * @return a Mono containing the found Account or empty if not found
     */
    Mono<BankAccount> findById(String id);

    /**
     * Retrieves all available accounts.
     *
     * @return a Flux containing all account responses
     */
    Flux<AccountResponse> findAll();

    /**
     * Deletes a account from the repository.
     *
     * @param bankAccount the account information to delete
     * @return a Mono representing the deletion operation
     */
    Mono<Void> delete(BankAccount bankAccount);

    /**
     * Checks if a bank account exists in the repository based on the client ID and account type.
     *
     * @param clientId the ID of the client
     * @param type the type of the bank account
     * @return a Mono containing a Boolean indicating whether the account exists or not
     */
    Mono<Boolean> existsByClientIdAndType(String clientId, String type);

}
