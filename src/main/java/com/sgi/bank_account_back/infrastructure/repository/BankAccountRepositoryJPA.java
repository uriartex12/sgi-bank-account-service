package com.sgi.bank_account_back.infrastructure.repository;

import com.sgi.bank_account_back.domain.model.BankAccount;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface BankAccountRepositoryJPA extends ReactiveMongoRepository<BankAccount,String> {

}
