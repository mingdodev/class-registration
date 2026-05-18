package com.example.classregistration.global.scheduler;

import com.example.classregistration.domain.enrollment.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnrollmentScheduler {

    private final EnrollmentService enrollmentService;

    @Scheduled(fixedRate = 3_600_000)
    public void cancelExpiredPendingEnrollments() {
        log.info("PENDING 자동 취소 스케줄러 실행");
        enrollmentService.findExpiredPendingEnrollmentIds().forEach(id -> {
            try {
                enrollmentService.cancelExpiredPendingEnrollment(id);
            } catch (Exception e) {
                log.error("만료 PENDING 취소 실패: enrollmentId={}", id, e);
            }
        });
    }
}
