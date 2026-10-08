package com.payflow.account.dto;

import com.payflow.account.entity.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AccountRequest {
    @NotNull(message = "Account Id is required")
    private Long accountId;

    @NotNull(message = "Customer Id is required")
    private Long customerId;

    @NotNull(message = "Account type is required")
    private AccountType accountType;

    @NotNull(message = "Balance is required")
    @DecimalMin(value="00.0",message = "Balance cannot be negative")
    private BigDecimal balance;

    @NotBlank(message = "Currency is required")
    private String currency;



}
