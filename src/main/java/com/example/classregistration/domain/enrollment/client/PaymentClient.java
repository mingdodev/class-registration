package com.example.classregistration.domain.enrollment.client;

public interface PaymentClient {
    boolean pay(Long orderId, int amount);
}
