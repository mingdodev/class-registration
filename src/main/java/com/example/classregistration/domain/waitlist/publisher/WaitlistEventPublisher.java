package com.example.classregistration.domain.waitlist.publisher;

public interface WaitlistEventPublisher {
    void publish(Long klassId);
}
