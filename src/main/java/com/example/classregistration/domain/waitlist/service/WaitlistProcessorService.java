package com.example.classregistration.domain.waitlist.service;

import com.example.classregistration.domain.enrollment.repository.EnrollmentRepository;
import com.example.classregistration.domain.enrollment.model.Enrollment;
import com.example.classregistration.domain.klass.repository.KlassRepository;
import com.example.classregistration.domain.klass.model.Klass;
import com.example.classregistration.domain.waitlist.repository.WaitlistRepository;
import com.example.classregistration.domain.waitlist.model.Waitlist;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 대기열 이벤트 하나를 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WaitlistProcessorService {

    private final WaitlistRepository waitlistRepository;
    private final KlassRepository klassRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional
    public boolean process(Long klassId) {
        Optional<Waitlist> entry = waitlistRepository.findFirstWaiterByKlassId(klassId);

        if (entry.isPresent()) {
            Waitlist waitlist = entry.get();
            int updated = klassRepository.decreaseRemainingCapacity(klassId);
            if (updated == 0) return false;

            Klass klass = klassRepository.getReferenceById(klassId);
            enrollmentRepository.save(Enrollment.create(waitlist.getKlassmate(), klass));
            waitlistRepository.delete(waitlist);
            return true;
        } else {
            // 대기열 소진: 아직 해당 강의의 수강 종료일이 지나지 않았으면 OPEN으로 전환
            klassRepository.findById(klassId).ifPresent(klass -> {
                if (!klass.isPeriodEnded()) klass.reopen();
            });
            return true;
        }
    }
}
