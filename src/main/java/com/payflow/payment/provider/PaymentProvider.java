package com.payflow.payment.provider;

import com.payflow.payment.entity.Payment;

public interface PaymentProvider {

    String getProviderName();

    String processPayment(Payment payment);
}