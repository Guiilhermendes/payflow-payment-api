package com.payflow.service;

import com.payflow.entity.Payment;
import com.payflow.enums.PaymentStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FakePaymentGateway implements PaymentGateway {
    private final PaymentStatus outcome;

    public FakePaymentGateway(@Value("${payflow.gateway.outcome:APPROVED}") PaymentStatus outcome) {
        this.outcome = outcome;
    }
    
    @Override public PaymentStatus process(Payment payment) { return outcome; }
}
