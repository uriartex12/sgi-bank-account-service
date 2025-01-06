package com.sgi.account.helper;

import com.sgi.account.domain.model.Balance;
import com.sgi.account.domain.model.BankAccount;
import com.sgi.account.infrastructure.dto.AccountRequest;
import com.sgi.account.infrastructure.dto.AccountResponse;
import com.sgi.account.infrastructure.dto.Credit;
import com.sgi.account.infrastructure.dto.Customer;
import com.sgi.account.infrastructure.dto.TransactionResponse;
import com.sgi.account.infrastructure.dto.BalanceResponse;
import com.sgi.account.infrastructure.dto.TransferRequest;
import com.sgi.account.infrastructure.dto.WithdrawalRequest;
import com.sgi.account.infrastructure.dto.DepositRequest;
import lombok.SneakyThrows;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.sgi.account.infrastructure.dto.AccountRequest.TypeEnum.CHECKING;
import static java.util.UUID.randomUUID;

/**
 * Class containing methods to generate AccountRequest and AccountResponse objects
 * with default values to facilitate unit testing.
 */
public class FactoryTest {

    /**
     * Generates an AccountRequest object with default values for testing.
     *
     * @return An AccountRequest object with configured values for testing.
     */
    @SneakyThrows
    public static <R> R toFactoryBankAccount(Class<R> response) {
        R account = response.getDeclaredConstructor().newInstance();
        if (account instanceof AccountRequest) {
            return (R) initializeAccount((AccountRequest) account);
        } else if (account instanceof AccountResponse) {
            return (R) initializeAccount((AccountResponse) account);
        }
        return account;
    }

    private static AccountRequest initializeAccount(AccountRequest account) {
        account.setIsActive(true);
        account.setClientId("client-test-0001");
        account.setCommissionFee(BigDecimal.valueOf(10));
        account.setCurrency("PEN");
        account.setType(CHECKING);
        account.setMovementLimit(10);
        account.setTransactionDay(LocalDate.now());
        account.setMaintenanceFee(BigDecimal.valueOf(5));
        account.setHolders(List.of("Jhon", "Carlos", "Jose", "Toledo"));
        return account;
    }

    private static AccountResponse initializeAccount(AccountResponse account) {
        account.setId(randomUUID().toString());
        account.setClientId("client-test-0001");
        account.setCommissionFee(BigDecimal.valueOf(10));
        account.setType(AccountResponse.TypeEnum.CHECKING);
        account.setMovementLimit(10);
        account.balance(BigDecimal.ZERO);
        account.setTransactionDay(LocalDate.now());
        account.setMaintenanceFee(BigDecimal.valueOf(5));
        account.setAccountNumber(randomUUID().toString());
        account.setHolders(List.of("Jhon", "Carlos", "Jose", "Toledo"));
        return account;
    }

    /**
     * Generates a list of AccountResponse objects with predefined values for testing purposes.
     *
     * @return A list of AccountResponse objects with default data.
     */
    public static List<AccountResponse> toFactoryListBankAccounts() {
        AccountResponse accountOne = new AccountResponse();
        accountOne.setId(randomUUID().toString());
        accountOne.setClientId("client-test-0001");
        accountOne.setCommissionFee(BigDecimal.valueOf(10));
        accountOne.setType(AccountResponse.TypeEnum.CHECKING);
        accountOne.setMovementLimit(10);
        accountOne.balance(BigDecimal.ZERO);
        accountOne.setTransactionDay(LocalDate.now());
        accountOne.setMaintenanceFee(BigDecimal.valueOf(5));
        accountOne.setAccountNumber(randomUUID().toString());
        accountOne.setHolders(List.of("Jhon", "Carlos", "Jose", "Toledo"));

        AccountResponse accountTwo = new AccountResponse();
        accountTwo.setId(randomUUID().toString());
        accountTwo.setClientId("client-test-0002");
        accountTwo.setCommissionFee(BigDecimal.valueOf(10));
        accountTwo.setType(AccountResponse.TypeEnum.SAVINGS);
        accountTwo.setMovementLimit(10);
        accountTwo.balance(BigDecimal.ZERO);
        accountTwo.setTransactionDay(LocalDate.now());
        accountTwo.setMaintenanceFee(BigDecimal.valueOf(5));
        accountTwo.setAccountNumber(randomUUID().toString());
        accountTwo.setHolders(List.of("Felix", "Carlos", "Andrea", "Toledo"));
        return List.of(accountOne, accountTwo);

    }

    /**
     * Creates a factory balance response with a default account balance and a random client ID.
     *
     * @return a {@link BalanceResponse} instance with predefined values.
     */
    public static BalanceResponse toFactoryBalanceClient() {
        BalanceResponse balance = new BalanceResponse();
        balance.setAccountBalance(BigDecimal.valueOf(1000));
        balance.setClientId(randomUUID().toString());
        return balance;
    }

    /**
     * Creates a deposit request with a default amount.
     *
     * @return A DepositRequest instance with a preset deposit amount.
     */
    public static DepositRequest toFactoryDepositRequest() {
        return new DepositRequest(100D);
    }

    /**
     * Creates a factory transaction response with a default client ID, a random product ID, and a fixed deposit amount.
     *
     * @param productId the product ID for the transaction.
     * @return a {@link TransactionResponse} instance with predefined values.
     */
    public static TransactionResponse toFactoryTransactionResponse(String productId) {
        TransactionResponse transactionResponse = new TransactionResponse();
        transactionResponse.setClientId(randomUUID().toString());
        transactionResponse.setAmount(BigDecimal.valueOf(100));
        transactionResponse.setType(TransactionResponse.TypeEnum.DEPOSIT);
        transactionResponse.setProductId(productId);
        transactionResponse.setDestinationProductId(null);
        return transactionResponse;
    }

        /**
         * Creates a list of TransactionResponse objects with default values for testing purposes.
         *
         * @param productId The product ID to associate with the transaction response.
         * @return A list containing a single TransactionResponse object with preset values.
         */
        public static List<TransactionResponse> toFactoryListTransactionResponse(String productId) {
            TransactionResponse transactionResponse = new TransactionResponse();
            transactionResponse.setClientId(randomUUID().toString());
            transactionResponse.setAmount(BigDecimal.valueOf(100));
            transactionResponse.setType(TransactionResponse.TypeEnum.DEPOSIT);
            transactionResponse.setProductId(productId);
            transactionResponse.setDestinationProductId(null);
            return List.of(transactionResponse);
        }

    /**
     * Creates a new transfer request with a random ID and a specified value.
     *
     * @return A TransferRequest object.
     */
    public static TransferRequest toFactoryTransferRequest(String destinationProductId) {
        return new TransferRequest(destinationProductId, 1D);
    }

    public static WithdrawalRequest toFactoryWithdrawalRequest() {
        return new WithdrawalRequest(100D);
    }

    /**
     * Creates a new BankAccount object with predefined values for testing purposes.
     *
     * @return A BankAccount object populated with random and default values, such as a client ID, balance,
     *         account type, and other attributes.
     */
    public static BankAccount toFactoryEntityBankAccount() {
        return BankAccount.builder()
                .id(randomUUID().toString())
                .accountBalance(Balance.builder()
                        .balance(BigDecimal.ONE)
                        .currency("PEN")
                        .build())
                .isActive(true)
                .clientId("client-test-0001")
                .commissionFee(BigDecimal.valueOf(10))
                .createdDate(Instant.now())
                .type("CHECKING")
                .movementLimit(10)
                .transactionDay(LocalDate.now())
                .movementsUsed(0)
                .holders(List.of("Jhon", "Carlos", "Jose", "Toledo"))
                .build();
    }

    /**
     * Creates a new Customer object with default values for testing purposes.
     * The customer will have a predefined ID, name, document ID, address, phone number,
     * profile, and email.
     *
     * @return A Customer object populated with test data.
     */
    public static Customer toFactoryCustomerResponse() {
        Customer customer = new Customer();
        customer.setId(randomUUID().toString());
        customer.setName("test");
        customer.setType(Customer.TypeEnum.PERSONAL);
        customer.setDocumentId("72923234");
        customer.setAddress("AV. Trujillo 516");
        customer.setPhoneNumber("910677465");
        customer.setProfile(Customer.ProfileEnum.VIP);
        customer.setEmail("test_1882@gmail.com");
        return customer;
    }

    /**
     * Creates a new Credit object with default values for testing purposes.
     * The credit object will have a random ID, credit limit, credit number, client ID,
     * interest rate, and balance.
     *
     * @return A Credit object populated with test data.
     */
    public static Credit toFactoryCreditResponse() {
        Credit credit = new Credit();
        credit.setId(randomUUID().toString());
        credit.setCreditLimit(BigDecimal.ONE);
        credit.setCreditNumber(UUID.randomUUID().toString());
        credit.setClientId(UUID.randomUUID().toString());
        credit.setType(Credit.TypeEnum.PERSONAL);
        credit.setInterestRate(BigDecimal.valueOf(10));
        credit.setBalance(BigDecimal.ZERO);
        return credit;
    }

    /**
     * Creates a new TransactionResponse object with default values for testing purposes.
     * The transaction will have a specified product ID, client ID, amount, and transaction type (DEPOSIT).
     *
     * @param productId The product ID associated with the transaction.
     * @param clientId  The client ID associated with the transaction.
     * @return A TransactionResponse object populated with test data.
     */
    public static TransactionResponse toFactoryToClientIdTransactionResponse(String productId, String clientId) {
        TransactionResponse transactionResponse = new TransactionResponse();
        transactionResponse.setProductId(productId);
        transactionResponse.setAmount(BigDecimal.TEN);
        transactionResponse.setClientId(clientId);
        transactionResponse.setType(TransactionResponse.TypeEnum.DEPOSIT);
        return transactionResponse;
    }

}
