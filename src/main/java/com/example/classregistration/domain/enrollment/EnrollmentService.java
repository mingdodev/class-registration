package com.example.classregistration.domain.enrollment;

import com.example.classregistration.domain.enrollment.dto.CreateEnrollmentResponse;
import com.example.classregistration.domain.enrollment.model.Enrollment;
import com.example.classregistration.domain.klass.KlassRepository;
import com.example.classregistration.domain.klass.model.Klass;
import com.example.classregistration.domain.klassmate.KlassmateRepository;
import com.example.classregistration.domain.klassmate.model.Klassmate;
import com.example.classregistration.domain.klass.model.KlassStatus;
import com.example.classregistration.domain.waitlist.WaitlistEventPublisher;
import com.example.classregistration.global.exception.BusinessException;
import com.example.classregistration.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final KlassRepository klassRepository;
    private final KlassmateRepository klassmateRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final WaitlistEventPublisher waitlistEventPublisher;

    @Transactional
    public CreateEnrollmentResponse enroll(Long klassmateId, Long klassId) {
        Klass klass = klassRepository.findById(klassId)
                .orElseThrow(() -> new BusinessException(ErrorCode.KLASS_NOT_FOUND));
        klass.validateEnrollable();

        if (enrollmentRepository.isAlreadyEnrolled(klassmateId, klassId)) {
            throw new BusinessException(ErrorCode.ENROLLMENT_ALREADY_EXISTS);
        }

        // 원자적 업데이트로 정원 차감. 0행 반환 시 정원 초과
        int updated = klassRepository.decreaseRemainingCapacity(klassId);
        if (updated == 0) throw new BusinessException(ErrorCode.KLASS_FULL);

        Klassmate klassmate = klassmateRepository.getReferenceById(klassmateId);
        Enrollment enrollment = enrollmentRepository.save(Enrollment.create(klassmate, klass));
        return new CreateEnrollmentResponse(enrollment.getId());
    }

    @Transactional
    public void confirmEnrollment(Long klassmateId, Long enrollmentId) {
        Enrollment enrollment = findEnrollment(klassmateId, enrollmentId);
        enrollment.confirm();
    }

    @Transactional
    public void cancelEnrollment(Long klassmateId, Long enrollmentId) {
        Enrollment enrollment = findEnrollment(klassmateId, enrollmentId);
        Long klassId = enrollment.getKlass().getId();
        KlassStatus klassStatus = enrollment.getKlass().getStatus();
        enrollment.cancel();
        klassRepository.increaseRemainingCapacity(klassId);

        // CLOSED 상태에서만 대기열 이벤트 발행
        if (klassStatus == KlassStatus.CLOSED) {
            try {
                waitlistEventPublisher.publish(klassId);
            } catch (Exception e) {
                log.error("대기열 이벤트 발행 실패: klassId={}", klassId, e);
            }
        }
    }

    public List<Long> findExpiredPendingEnrollmentIds() {
        return enrollmentRepository.findExpiredPendingEnrollments(LocalDateTime.now().minusHours(24))
                .stream().map(Enrollment::getId).toList();
    }

    @Transactional
    public void cancelExpiredPendingEnrollment(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENROLLMENT_NOT_FOUND));
        Long klassId = enrollment.getKlass().getId();
        KlassStatus klassStatus = enrollment.getKlass().getStatus();
        enrollment.expirePayment();
        klassRepository.increaseRemainingCapacity(klassId);

        if (klassStatus == KlassStatus.CLOSED) {
            try {
                waitlistEventPublisher.publish(klassId);
            } catch (Exception e) {
                log.error("대기열 이벤트 발행 실패: klassId={}", klassId, e);
            }
        }
    }

    private Enrollment findEnrollment(Long klassmateId, Long enrollmentId) {
        return enrollmentRepository.findByIdAndKlassmateId(enrollmentId, klassmateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENROLLMENT_NOT_FOUND));
    }
}
