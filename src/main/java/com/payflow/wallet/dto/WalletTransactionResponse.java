
package com.payflow.wallet.dto;

import com.payflow.wallet.entity.WalletTransaction;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WalletTransactionResponse {

    private Long transactionId;
    private WalletTransaction.TransactionType type;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private WalletTransaction.ReferenceType referenceType;
    private Long referenceId;
    private String description;
    private LocalDateTime createdAt;

    public WalletTransactionResponse() {
    }

    public static WalletTransactionResponse from(
            WalletTransaction transaction
    ) {
        WalletTransactionResponse response =
                new WalletTransactionResponse();

        response.transactionId = transaction.getId();
        response.type = transaction.getType();
        response.amount = transaction.getAmount();
        response.balanceBefore = transaction.getBalanceBefore();
        response.balanceAfter = transaction.getBalanceAfter();
        response.referenceType = transaction.getReferenceType();
        response.referenceId = transaction.getReferenceId();
        response.description = transaction.getDescription();
        response.createdAt = transaction.getCreatedAt();

        return response;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public WalletTransaction.TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceBefore() {
        return balanceBefore;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public WalletTransaction.ReferenceType getReferenceType() {
        return referenceType;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
