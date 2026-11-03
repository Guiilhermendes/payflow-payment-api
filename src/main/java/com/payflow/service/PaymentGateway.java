package com.payflow.service;

import com.payflow.enums.PaymentStatus;
import com.payflow.entity.Payment;

public interface PaymentGateway {
    PaymentStatus process(Payment payment);
}
