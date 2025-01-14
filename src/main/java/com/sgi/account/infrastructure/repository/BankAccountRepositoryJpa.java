package com.sgi.account.infrastructure.repository;

import com.sgi.account.domain.model.BankAccount;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repositorio Reactivo para la entidad Account.
 * Extiende de ReactiveMongoRepository para realizar operaciones CRUD en MongoDB.
 */
public interface BankAccountRepositoryJpa extends ReactiveMongoRepository<BankAccount, String> {

    Mono<Boolean> existsByClientIdAndType(String clientId, String type);

    Flux<BankAccount> findAllByClientIdOrTypeOrId(String clientId, String type, String id);
}
