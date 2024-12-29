package com.sgi.account.infrastructure.exception;

import com.sgi.account.infrastructure.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * GlobalExceptionHandler maneja las excepciones personalizadas en la aplicación.
 * Utiliza @ControllerAdvice para interceptar las excepciones lanzadas y generar respuestas de error adecuadas.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Maneja la excepción personalizada y devuelve una respuesta con el código de estado y el mensaje de error.
     *
     * @param ex Excepción personalizada.
     * @return Mono que encapsula la respuesta de error.
     */
    @ExceptionHandler(CustomException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleCustomException(CustomException ex) {
        return Mono.just(ResponseEntity
                .status(ex.getStatus())
                .body(createErrorResponse(ex)));
    }

    /**
     * Crea un objeto ErrorResponse a partir de la excepción personalizada.
     *
     * @param ex Excepción personalizada.
     * @return Un objeto ErrorResponse con los detalles del error.
     */
    private ErrorResponse createErrorResponse(CustomException ex) {
        LocalDateTime localDateTime = ex.getTimestamp();
        OffsetDateTime offsetDateTime = localDateTime.atOffset(ZoneOffset.UTC);
        return new ErrorResponse(ex.getStatus(), ex.getCode(), ex.getMessage(), offsetDateTime);
    }
}