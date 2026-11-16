package com.payflow.payment.provider;

import com.payflow.payment.entity.Payment;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MockPaymentProvider implements PaymentProvider {

    @Override
    public String getProviderName() {
        return "MOCK";
    }

    @Override
    public String processPayment(Payment payment) {

        return "mock_" + UUID.randomUUID();
    }
}