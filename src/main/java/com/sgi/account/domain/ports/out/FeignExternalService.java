package com.sgi.account.domain.ports.out;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Interface for making reactive POST and GET HTTP requests to external services.
 * Defines methods for sending and receiving data asynchronously using Mono and Flux.
 */
public interface FeignExternalService {
    <T, R> Mono<R> post(String url, T requestBody, Class<R> responseType);
    <R> Flux<R> getFlux(String url,  String pathVariable, Class<R> responseType);
    <R> Mono<R> getMono(String url, String pathVariable, Class<R> responseType);
}
