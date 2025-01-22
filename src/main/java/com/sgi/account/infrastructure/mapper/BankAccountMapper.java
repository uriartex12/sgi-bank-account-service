package com.sgi.account.infrastructure.mapper;

import com.sgi.account.domain.model.BankAccount;
import com.sgi.account.infrastructure.dto.AccountBalanceResponse;
import com.sgi.account.infrastructure.dto.AccountRequest;
import com.sgi.account.infrastructure.dto.AccountResponse;
import com.sgi.account.infrastructure.dto.BalanceResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Mapper para la conversión entre objetos Account y otros DTOs como AccountRequest, AccountResponse y BalanceResponse.
 * Utiliza MapStruct para automatizar la conversión de tipos entre objetos.
 */
@Mapper
public interface BankAccountMapper {

    BankAccountMapper INSTANCE = Mappers.getMapper(BankAccountMapper.class);

    @Mapping(target = "type", source = "type")
    @Mapping(target = "balance", source = "accountBalance")
    AccountResponse toAccountResponse(BankAccount bankAccount);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "type", source = "type")
    BankAccount toAccount(AccountRequest accountRequest);

    @Mapping(target = "accountId", source = "id")
    @Mapping(target = "accountBalance", source = "accountBalance.balance")
    BalanceResponse toBalance(BankAccount bankAccount);

    @Mapping(target = "status", source = "status")
    AccountBalanceResponse toAccountBalance(AccountResponse accountResponse, String status);

    default OffsetDateTime map(Instant instant) {
        return instant != null ? instant.atOffset(ZoneOffset.UTC) : null;
    }
}
