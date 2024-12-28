package com.sgi.account.infrastructure.feign;
import com.sgi.account.domain.ports.out.FeignExternalService;
import com.sgi.account.domain.shared.CustomError;
import com.sgi.account.infrastructure.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.sgi.account.domain.shared.Constants.EXTERNAL_REQUEST_ERROR_FORMAT;
import static com.sgi.account.domain.shared.Constants.EXTERNAL_REQUEST_SUCCESS_FORMAT;

/**
 * Implementación del servicio externo Feign para realizar solicitudes HTTP.
 * Utiliza WebClient para hacer solicitudes reactivas a un servicio externo.
 */
@Slf4j
@Service
public class FeignExternalServiceImpl implements FeignExternalService {

    private final WebClient webClient;

    public FeignExternalServiceImpl(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * Realiza una solicitud POST al servicio externo.
     *
     * @param url La URL del servicio.
     * @param requestBody El cuerpo de la solicitud.
     * @param responseType El tipo de la respuesta esperada.
     * @param <T> Tipo del cuerpo de la solicitud.
     * @param <R> Tipo de la respuesta.
     * @return Mono que encapsula la respuesta.
     */
    @Override
    public <T, R> Mono<R> post(String url, T requestBody, Class<R> responseType) {
        return webClient.post()
                .uri(url)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(responseType)
                .doOnNext(response -> logSuccess(url, response))
                .doOnError(ex ->
                        log.error(EXTERNAL_REQUEST_ERROR_FORMAT, url, ex))
                .onErrorResume(ex -> {
                    log.error(EXTERNAL_REQUEST_ERROR_FORMAT, url, ex);
                    return Mono.error(new CustomException(CustomError.E_OPERATION_FAILED));
                });
    }

    /**
     * Realiza una solicitud GET al servicio externo.
     *
     * @param url La URL del servicio.
     * @param pathVariable El valor de la variable de la URL.
     * @param responseType El tipo de la respuesta esperada.
     * @param <R> Tipo de la respuesta.
     * @return Flux que encapsula la respuesta.
     */
    @Override
    public <R> Flux<R> getFlux(String url, String pathVariable, Class<R> responseType) {
        return webClient.get()
                .uri(url, pathVariable)
                .retrieve()
                .bodyToFlux(responseType)
                .doOnNext(response -> logSuccess(url, response))
                .doOnError(ex ->
                        log.error(EXTERNAL_REQUEST_ERROR_FORMAT, url, ex))
                .onErrorResume(ex ->
                        Flux.error(new CustomException(CustomError.E_OPERATION_FAILED)));
    }

    /**
     * Realiza una solicitud GET al servicio externo.
     *
     * @param url La URL del servicio.
     * @param pathVariable El valor de la variable de la URL.
     * @param responseType El tipo de la respuesta esperada.
     * @param <R> Tipo de la respuesta.
     * @return Mono que encapsula la respuesta.
     */
    @Override
    public <R> Mono<R> getMono(String url, String pathVariable, Class<R> responseType) {
        return webClient.get()
                .uri(url, pathVariable)
                .retrieve()
                .bodyToMono(responseType)
                .doOnNext(response -> logSuccess(url, response))
                .doOnError(ex ->
                        log.error(EXTERNAL_REQUEST_ERROR_FORMAT, url, ex))
                .onErrorResume(ex -> {
                        log.error(EXTERNAL_REQUEST_ERROR_FORMAT, url, ex);
                    return Mono.error(new CustomException(CustomError.E_OPERATION_FAILED));
                });
    }

    private <R> void logSuccess(String url, R response) {
        log.info(EXTERNAL_REQUEST_SUCCESS_FORMAT, url, response);
    }
}
