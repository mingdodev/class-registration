package com.example.classregistration.global.scheduler;

import com.example.classregistration.domain.enrollment.EnrollmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnrollmentScheduler {

    private final EnrollmentService enrollmentService;

    // 1시간마다 결제 기한을 초과한 수강 신청(PENDING)을 자동 취소
    @Scheduled(fixedRate = 3_600_000)
    public void cancelExpiredPendingEnrollments() {
        log.info("PENDING 자동 취소 스케줄러 실행");
        enrollmentService.cancelExpiredPendingEnrollments();
    }
}
