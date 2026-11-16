
package com.payflow.wallet.dto;

import com.payflow.wallet.entity.WalletDeposit;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WalletDepositResponse {

    private Long depositId;
    private BigDecimal amount;
    private String currency;
    private String provider;
    private String providerReference;
    private String status;
    private LocalDateTime createdAt;

    public WalletDepositResponse() {
    }

    public static WalletDepositResponse from(WalletDeposit deposit) {
        WalletDepositResponse response = new WalletDepositResponse();

        response.setDepositId(deposit.getId());
        response.setAmount(deposit.getAmount());
        response.setCurrency(deposit.getCurrency());
        response.setProvider(deposit.getProvider());
        response.setProviderReference(deposit.getProviderReference());
        response.setStatus(deposit.getStatus().name());
        response.setCreatedAt(deposit.getCreatedAt());

        return response;
    }

    public Long getDepositId() {
        return depositId;
    }

    public void setDepositId(Long depositId) {
        this.depositId = depositId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getProviderReference() {
        return providerReference;
    }

    public void setProviderReference(String providerReference) {
        this.providerReference = providerReference;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
