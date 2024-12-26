package com.sgi.bank_account_back.infrastructure.mapper;

import com.sgi.bank_account_back.domain.model.BankAccount;
import com.sgi.bank_account_back.infrastructure.dto.AccountRequest;
import com.sgi.bank_account_back.infrastructure.dto.AccountResponse;
import com.sgi.bank_account_back.infrastructure.dto.BalanceResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Mapper
public interface BankAccountMapper {

    BankAccountMapper INSTANCE = Mappers.getMapper(BankAccountMapper.class);

    @Mapping(target = "type", source = "type")
    @Mapping(target = "balance", source = "accountBalance")
    AccountResponse map(BankAccount bankAccount);

    @Mapping(target = "id", ignore = true)
    BankAccount map(AccountRequest accountRequest);

    @Mapping(target = "accountBalance", source = "accountBalance.balance")
    BalanceResponse balance (BankAccount bankAccount);

    default Mono<BankAccount> map(Mono<AccountRequest> accountRequestMono) {
        return accountRequestMono.map(this::map);
    }

    default OffsetDateTime map(Instant instant) {
        return instant != null ? instant.atOffset(ZoneOffset.UTC) : null;
    }
}
