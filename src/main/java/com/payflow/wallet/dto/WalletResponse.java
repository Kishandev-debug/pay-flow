
package com.payflow.wallet.dto;

import com.payflow.wallet.entity.Wallet;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WalletResponse {

    private Long walletId;
    private BigDecimal balance;
    private String currency;
    private LocalDateTime createdAt;

    public WalletResponse() {
    }

    public WalletResponse(
            Long walletId,
            BigDecimal balance,
            String currency,
            LocalDateTime createdAt
    ) {
        this.walletId = walletId;
        this.balance = balance;
        this.currency = currency;
        this.createdAt = createdAt;
    }

    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getBalance(),
                wallet.getCurrency(),
                wallet.getCreatedAt()
        );
    }

    public Long getWalletId() {
        return walletId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public String getCurrency() {
        return currency;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
