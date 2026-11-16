
package com.payflow.wallet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;

public class WalletDepositRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(
            value = "0.01",
            message = "Deposit amount must be greater than zero"
    )
    @Digits(
            integer = 17,
            fraction = 2,
            message = "Deposit amount supports up to two decimal places"
    )
    private BigDecimal amount;

    public WalletDepositRequest() {
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
