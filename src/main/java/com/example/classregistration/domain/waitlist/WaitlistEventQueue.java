package com.example.classregistration.domain.waitlist;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class WaitlistEventQueue implements WaitlistEventPublisher {

    // ADR-04: 강의별 독립 인메모리 큐
    private final ConcurrentHashMap<Long, ConcurrentLinkedQueue<Long>> queues = new ConcurrentHashMap<>();

    @Override
    public void publish(Long klassId) {
        queues.computeIfAbsent(klassId, k -> new ConcurrentLinkedQueue<>()).add(klassId);
    }
}
