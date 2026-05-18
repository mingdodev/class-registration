package com.example.classregistration.global.payment;

public interface PaymentClient {
    boolean pay(Long orderId, int amount);
}
