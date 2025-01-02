package com.sgi.account.infrastructure.feign;

import com.sgi.account.domain.ports.out.FeignExternalService;
import com.sgi.account.domain.shared.CustomError;
import com.sgi.account.infrastructure.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.sgi.account.domain.shared.Constants.EXTERNAL_REQUEST_ERROR_FORMAT;
import static com.sgi.account.domain.shared.Constants.EXTERNAL_REQUEST_SUCCESS_FORMAT;

/**
 * Implementación del servicio externo Feign para realizar solicitudes HTTP de manera reactiva con soporte de Circuit Breaker.
 */
@Slf4j
@Service
public class FeignExternalServiceImpl implements FeignExternalService {

    private final WebClient webClient;
    private final ReactiveCircuitBreaker circuitBreaker;

    public FeignExternalServiceImpl(WebClient.Builder webClientBuilder, ReactiveCircuitBreakerFactory circuitBreakerFactory) {
        this.webClient = webClientBuilder.build();
        this.circuitBreaker = circuitBreakerFactory.create("account-service");
    }

    @Override
    public <T, R> Mono<R> post(String url, T requestBody, Class<R> responseType) {
        return webClient.post()
                .uri(url)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(responseType)
                .doOnNext(response -> logSuccess(url, response))
                .doOnError(ex -> logError(url, ex))
                .onErrorResume(ex -> Mono.error(new CustomException(CustomError.E_OPERATION_FAILED)))
                .transformDeferred(circuitBreaker::run);
    }

    @Override
    public <R> Flux<R> getFlux(String url, String pathVariable, Class<R> responseType) {
        return webClient.get()
                .uri(url, pathVariable)
                .retrieve()
                .bodyToFlux(responseType)
                .doOnNext(response -> logSuccess(url, response))
                .doOnError(ex -> logError(url, ex))
                .onErrorResume(ex -> Flux.error(new CustomException(CustomError.E_OPERATION_FAILED)))
                .transformDeferred(circuitBreaker::run);
    }

    @Override
    public <R> Mono<R> getMono(String url, String pathVariable, Class<R> responseType) {
        return webClient.get()
                .uri(url, pathVariable)
                .retrieve()
                .bodyToMono(responseType)
                .doOnNext(response -> logSuccess(url, response))
                .doOnError(ex -> logError(url, ex))
                .onErrorResume(ex -> Mono.error(new CustomException(CustomError.E_OPERATION_FAILED)))
                .transformDeferred(circuitBreaker::run);
    }

    private <R> void logSuccess(String url, R response) {
        log.info(EXTERNAL_REQUEST_SUCCESS_FORMAT, url, response);
    }

    private void logError(String url, Throwable ex) {
        log.error(EXTERNAL_REQUEST_ERROR_FORMAT, url, ex);
    }
}
