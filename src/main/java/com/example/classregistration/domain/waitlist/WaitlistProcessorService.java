package com.example.classregistration.domain.waitlist;

import com.example.classregistration.domain.enrollment.EnrollmentRepository;
import com.example.classregistration.domain.enrollment.model.Enrollment;
import com.example.classregistration.domain.klass.KlassRepository;
import com.example.classregistration.domain.klass.model.Klass;
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

    /**
     * 강의의 대기열 이벤트를 처리한다.
     * @return 처리 성공 여부 (false면 이벤트를 큐에서 제거하지 않고 재시도)
     */
    @Transactional
    public boolean process(Long klassId) {
        Optional<Waitlist> entry = waitlistRepository.findFirstWaiterByKlassId(klassId);

        if (entry.isPresent()) {
            Waitlist waitlist = entry.get();
            klassRepository.decreaseRemainingCapacity(klassId);

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
