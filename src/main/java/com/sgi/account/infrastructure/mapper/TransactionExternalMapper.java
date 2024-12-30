package com.sgi.account.infrastructure.mapper;

import com.sgi.account.domain.model.BankAccount;
import com.sgi.account.infrastructure.dto.TransactionRequest;
import jakarta.validation.constraints.NotNull;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;

import static com.sgi.account.infrastructure.dto.TransactionRequest.TypeEnum;

/**
 * Mapper for converting {@link BankAccount} and {@link TransactionRequest}.
 * This interface is used by MapStruct to generate the implementation for mapping between the domain and DTO objects.
 */
@Mapper
public interface TransactionExternalMapper {

    /**
     * Instance of the {@link TransactionExternalMapper} for performing the mapping.
     */
    TransactionExternalMapper INSTANCE = Mappers.getMapper(TransactionExternalMapper.class);

    /**
     * Maps a {@link BankAccount} object to a {@link TransactionRequest} DTO.
     *
     * @param account           the source {@link BankAccount} object to be mapped
     * @param destinationProduct the destination product ID for the transaction
     * @param amount            the amount to be transferred
     * @param type              the type of the transaction (either withdrawal or deposit)
     * @param balance           the balance after the transaction
     * @return a {@link TransactionRequest} containing the mapped data
     */
    default TransactionRequest map(BankAccount account, @NotNull String destinationProduct, BigDecimal amount,
                                   TypeEnum type, BigDecimal balance) {
        TransactionRequest transaction = new TransactionRequest();
        transaction.setProductId(account.getId());
        transaction.setDestinationProductId(destinationProduct);
        transaction.setClientId(account.getClientId());
        transaction.setAmount(amount.doubleValue());
        transaction.setBalance(balance.doubleValue());
        transaction.setType(type);
        return transaction;
    }

}
