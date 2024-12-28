package com.sgi.account.domain.model;

import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

/**
 * Representa el balance de una cuenta bancaria, incluyendo el saldo y la moneda en la que se expresa.
 * Esta clase permite almacenar y manipular el balance de una cuenta, así como la moneda asociada a él.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Balance {

    private BigDecimal balance;
    private String currency;

    /**
     * Constructor para crear un Balance con un saldo específico.
     *
     * @param balance El saldo de la cuenta.
     */
    public Balance(BigDecimal balance) {
        this.balance = balance;
    }
}
