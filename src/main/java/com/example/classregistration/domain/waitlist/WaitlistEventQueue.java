package com.example.classregistration.domain.waitlist;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 대기열 처리를 트리거하는 이벤트를 저장하는 인메모리 큐.
 * 강의별로 독립된 ConcurrentLinkedQueue를 사용해 처리 실패가 다른 강의에 영향을 주지 않는다.
 * reference docs: ADR-04.md
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WaitlistEventQueue implements WaitlistEventPublisher {

    private static final int FAILURE_THRESHOLD = 3;

    private final WaitlistProcessorService processorService;


    private final ConcurrentHashMap<Long, ConcurrentLinkedQueue<Long>> queues = new ConcurrentHashMap<>(); // 강의별 독립 인메모리 큐
    private final ConcurrentHashMap<Long, AtomicBoolean> consumers = new ConcurrentHashMap<>(); // 강의별 소비자 스레드 실행 여부 (강의당 소비자 하나 보장)
    private final ConcurrentHashMap<Long, CircuitBreaker> circuits = new ConcurrentHashMap<>(); // 강의별 서킷 브레이커

    @Override
    public void publish(Long klassId) {
        queues.computeIfAbsent(klassId, k -> new ConcurrentLinkedQueue<>()).add(klassId);
        startConsumerIfIdle(klassId);
    }

    // 스케줄러: 서킷이 OPEN된 강의의 대기열 처리를 재시도한다 (서킷 HALF_OPEN)
    public void retryOpenCircuits() {
        circuits.entrySet().stream()
                .filter(e -> !e.getValue().allowRequest())
                .map(java.util.Map.Entry::getKey)
                .forEach(klassId -> {
                    circuits.get(klassId).halfOpen();
                    startConsumerIfIdle(klassId);
                });
    }

    private void startConsumerIfIdle(Long klassId) {
        if (!circuits.computeIfAbsent(klassId, k -> new CircuitBreaker()).allowRequest()) return;

        AtomicBoolean running = consumers.computeIfAbsent(klassId, k -> new AtomicBoolean(false));
        if (running.compareAndSet(false, true)) {
            // 가상 스레드로 소비자 생성
            Thread.ofVirtual().start(() -> consume(klassId, running));
        }
    }

    // peek → 처리 성공 → poll
    private void consume(Long klassId, AtomicBoolean running) {
        ConcurrentLinkedQueue<Long> queue = queues.get(klassId);
        CircuitBreaker circuit = circuits.get(klassId);

        try {
            while (queue != null && queue.peek() != null && circuit.allowRequest()) {
                boolean success = tryProcess(klassId);
                if (success) {
                    queue.poll();
                    circuit.recordSuccess();
                    continue;
                }

                // 실패 시 1회 재시도
                boolean retrySuccess = tryProcess(klassId);
                if (retrySuccess) {
                    queue.poll();
                    circuit.recordSuccess();
                } else {
                    circuit.recordFailure();
                    log.error("대기열 처리 재시도 실패 - klassId={}, 누적 실패={}", klassId, circuit.failureCount);
                }
            }
        } finally {
            running.set(false);
        }
    }

    private boolean tryProcess(Long klassId) {
        try {
            return processorService.process(klassId);
        } catch (Exception e) {
            log.error("대기열 처리 중 예외 발생 - klassId={}", klassId, e);
            return false;
        }
    }

    // 강의별 서킷 브레이커 상태 관리
    static class CircuitBreaker {

        enum State { CLOSED, OPEN, HALF_OPEN }

        private volatile State state = State.CLOSED;
        private volatile int failureCount = 0;

        boolean allowRequest() {
            return state == State.CLOSED || state == State.HALF_OPEN;
        }

        void recordSuccess() {
            failureCount = 0;
            state = State.CLOSED;
        }

        void recordFailure() {
            if (++failureCount >= FAILURE_THRESHOLD) {
                state = State.OPEN;
            }
        }

        void halfOpen() {
            state = State.HALF_OPEN;
        }
    }
}
