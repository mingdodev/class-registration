package com.example.classregistration.domain.enrollment.client;

import org.springframework.stereotype.Component;

@Component
public class FakePaymentClient implements PaymentClient {

    @Override
    public boolean pay(Long orderId, int amount) {
        return true;
    }
}
