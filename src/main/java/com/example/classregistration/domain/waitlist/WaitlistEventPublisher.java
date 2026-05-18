package com.example.classregistration.domain.waitlist;

public interface WaitlistEventPublisher {
    void publish(Long klassId);
}
