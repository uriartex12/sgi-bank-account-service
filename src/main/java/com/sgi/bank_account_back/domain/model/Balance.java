package com.sgi.bank_account_back.domain.model;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Balance {
    
    private BigDecimal balance;
    private String currency;

    public Balance(BigDecimal balance){
        this.balance = balance;
    }
}
