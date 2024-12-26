package com.sgi.bank_account_back.domain.ports.out;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FeignExternalService {
    <T, R> Mono<R> post(String url, T requestBody, Class<R> responseType);
    <R> Flux<R> getFlux(String url,  String pathVariable, Class<R> responseType);
    <R> Mono<R> getMono(String url, String pathVariable, Class<R> responseType);
}
