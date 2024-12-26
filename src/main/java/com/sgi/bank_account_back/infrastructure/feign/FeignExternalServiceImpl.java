package com.sgi.bank_account_back.infrastructure.feign;
import com.sgi.bank_account_back.domain.ports.out.FeignExternalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class FeignExternalServiceImpl implements FeignExternalService {

    private final WebClient webClient;

    public FeignExternalServiceImpl(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @Override
    public <T, R> Mono<R> post(String url, T requestBody, Class<R> responseType) {
        return webClient.post()
                .uri(url)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(responseType)
                .doOnNext(response ->
                        log.info("POST request to '{}' succeeded: {}", url, response))
                .doOnError(ex ->
                        log.error("Error during POST request to '{}'", url, ex))
                .onErrorMap(ex ->
                        new Exception("Error processing POST request", ex));
    }

    @Override
    public <R> Flux<R> getFlux(String url, String pathVariable, Class<R> responseType) {
        return webClient.get()
                .uri(url, pathVariable)
                .retrieve()
                .bodyToFlux(responseType)
                .doOnNext(response ->
                        log.info("GET request to '{}' succeeded: {}", url, response))
                .doOnError(ex ->
                        log.error("Error during GET request to '{}'", url, ex))
                .onErrorResume(ex ->
                        Flux.error(new Exception("Error processing GET request", ex)));
    }

    @Override
    public <R> Mono<R> getMono(String url, String pathVariable, Class<R> responseType) {
        return webClient.get()
                .uri(url, pathVariable)
                .retrieve()
                .bodyToMono(responseType)
                .doOnNext(response ->
                        log.info("GET request to '{}' succeeded: {}", url, response))
                .doOnError(ex ->
                        log.error("Error during GET request to '{}'", url, ex))
                .onErrorResume(ex ->
                        Mono.error(new Exception("Error processing GET request", ex)));
    }
}
