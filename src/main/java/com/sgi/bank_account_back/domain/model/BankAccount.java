package com.sgi.bank_account_back.domain.model;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "bank-account")
@CompoundIndex(def = "{'id': 1, 'accountNumber': 1}", name = "id_accountNumber_index", unique = true)
public class BankAccount {

    @Id
    private String id;
    private String accountNumber;
    private String type;
    private String clientId;
    private Balance accountBalance;
    private BigDecimal maintenanceFee;
    private Integer movementLimit;
    private Integer movementsUsed;
    private Boolean isActive;
    private List<String> authorizedSigners;
    private List<String> holders;
    private LocalDate transactionDay;
    @CreatedDate
    private Instant createdDate;
    @LastModifiedDate
    private Instant updatedDate;

}
