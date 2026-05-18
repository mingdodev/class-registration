package com.example.classregistration.global.scheduler;

import com.example.classregistration.domain.klass.KlassService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KlassScheduler {

    private final KlassService klassService;

    // 24시간 주기로 수강 종료일이 지난 OPEN 강의를 CLOSED로 전환
    @Scheduled(fixedRate = 86_400_000)
    public void closeExpiredKlasses() {
        log.info("OPEN → CLOSED 자동 전환 스케줄러 실행");
        klassService.closeExpiredKlasses();
    }
}
