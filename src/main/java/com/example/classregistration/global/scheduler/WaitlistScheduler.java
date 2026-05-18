package com.example.classregistration.global.scheduler;

import com.example.classregistration.domain.waitlist.publisher.WaitlistEventQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WaitlistScheduler {

    private final WaitlistEventQueue waitlistEventQueue;

    // 5분 주기로 서킷이 OPEN된 강의의 대기열 처리를 재시도(HALF_OPEN)
    @Scheduled(fixedRate = 300_000)
    public void retryOpenCircuits() {
        waitlistEventQueue.retryOpenCircuits();
    }
}
