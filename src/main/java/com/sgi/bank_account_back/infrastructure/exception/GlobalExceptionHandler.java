package com.sgi.bank_account_back.infrastructure.exception;

import com.sgi.bank_account_back.infrastructure.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleCustomException(CustomException ex) {
        return Mono.just(ResponseEntity
                .status(ex.getStatus())
                .body(createErrorResponse(ex)));
    }

    private ErrorResponse createErrorResponse(CustomException ex) {
        LocalDateTime localDateTime = ex.getTimestamp();
        OffsetDateTime offsetDateTime = localDateTime.atOffset(ZoneOffset.UTC);
        return new ErrorResponse(ex.getStatus(), ex.getCode(), ex.getMessage(),offsetDateTime);
    }
}
