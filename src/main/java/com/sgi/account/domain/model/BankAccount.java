package com.sgi.account.domain.model;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Represents a bank account in the system.
 * This class contains the information related to a bank account,
 * including the account number, account type, balance, maintenance fees,
 * and other relevant account details.
 * The class is mapped to the 'bank-account' collection in the MongoDB database.
 */
@Getter
@Setter
@Builder
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
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal maintenanceFee;
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal commissionFee;
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
