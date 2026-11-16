package com.payflow.refund.dto;

import com.payflow.refund.entity.RefundStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RefundResponse {

    private Long id;
    private Long paymentId;
    private BigDecimal amount;
    private RefundStatus status;
    private String providerRefundId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public RefundResponse() {
    }

    public RefundResponse(
            Long id,
            Long paymentId,
            BigDecimal amount,
            RefundStatus status,
            String providerRefundId,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.paymentId = paymentId;
        this.amount = amount;
        this.status = status;
        this.providerRefundId = providerRefundId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public RefundStatus getStatus() {
        return status;
    }

    public String getProviderRefundId() {
        return providerRefundId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}