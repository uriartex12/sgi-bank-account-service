package com.sgi.account.domain.model;

import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

/**
 * Represents the balance of a bank account, including the balance and the currency in which it is expressed.
 * This class allows you to store and manipulate the balance of an account, as well as the currency associated with it.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Balance {

    private BigDecimal balance;
    private String currency;

}
