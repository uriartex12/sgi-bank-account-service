package com.sgi.bank_account_back.infrastructure.exception;

import com.sgi.bank_account_back.domain.shared.CustomError;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class CustomException extends RuntimeException{
    private Integer status;
    private String message;
    private String code;
    private LocalDateTime timestamp;

    public CustomException(CustomError error) {
        this.status = error.getError().getStatus().value();
        this.message = error.getError().getMessage();
        this.code = error.getError().getCode();
        this.timestamp = LocalDateTime.now();
    }
}
