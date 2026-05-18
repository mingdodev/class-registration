package com.example.classregistration.domain.klass;

import com.example.classregistration.domain.creator.CreatorRepository;
import com.example.classregistration.domain.creator.model.Creator;
import com.example.classregistration.domain.enrollment.EnrollmentRepository;
import com.example.classregistration.domain.enrollment.model.Enrollment;
import com.example.classregistration.domain.klass.dto.CreateKlassRequest;
import com.example.classregistration.domain.klass.dto.UpdateKlassRequest;
import com.example.classregistration.domain.klass.model.Klass;
import com.example.classregistration.domain.klass.model.KlassStatus;
import com.example.classregistration.domain.waitlist.WaitlistEventPublisher;
import com.example.classregistration.global.exception.BusinessException;
import com.example.classregistration.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KlassService {

    private final KlassRepository klassRepository;
    private final CreatorRepository creatorRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final WaitlistEventPublisher waitlistEventPublisher;

    @Transactional
    public Long createKlass(Long creatorId, CreateKlassRequest request) {
        Creator creator = creatorRepository.getReferenceById(creatorId);
        Klass klass = Klass.create(creator, request.title(), request.description(),
                request.price(), request.maxCapacity(), request.startDate(), request.endDate());
        return klassRepository.save(klass).getId();
    }

    @Transactional
    public void openKlass(Long creatorId, Long klassId) {
        Klass klass = findKlassById(klassId);
        validateOwnership(klass, creatorId);
        klass.open();
    }

    @Transactional
    public void updateKlass(Long creatorId, Long klassId, UpdateKlassRequest request) {
        Klass klass = findKlassById(klassId);
        validateOwnership(klass, creatorId);
        int oldMaxCapacity = klass.getMaxCapacity();
        klass.update(request.title(), request.description(), request.price(),
                request.maxCapacity(), request.startDate(), request.endDate());

        // CLOSED 상태에서 정원 증가 시 증가한 수만큼 이벤트 발행
        if (klass.getStatus() == KlassStatus.CLOSED && request.maxCapacity() != null) {
            int added = klass.getMaxCapacity() - oldMaxCapacity;
            for (int i = 0; i < added; i++) {
                waitlistEventPublisher.publish(klassId);
            }
        }
    }

    @Transactional
    public void deleteKlass(Long creatorId, Long klassId) {
        Klass klass = findKlassById(klassId);
        validateOwnership(klass, creatorId);
        klass.validateDeletable();
        klassRepository.delete(klass);
    }

    public Klass getKlass(Long klassId) {
        return findKlassById(klassId);
    }

    public List<Klass> getCreatorKlasses(Long creatorId, KlassStatus status) {
        return (status == null)
                ? klassRepository.findByCreatorId(creatorId)
                : klassRepository.findByCreatorIdAndStatus(creatorId, status);
    }

    public List<Enrollment> getKlassmates(Long creatorId, Long klassId) {
        Klass klass = findKlassById(klassId);
        validateOwnership(klass, creatorId);
        return enrollmentRepository.findRegisteredEnrollmentsByKlassId(klassId);
    }

    @Transactional
    public void closeExpiredKlasses() {
        klassRepository.findExpiredOpenKlasses(LocalDate.now())
                .forEach(Klass::close);
    }

    private Klass findKlassById(Long klassId) {
        return klassRepository.findById(klassId)
                .orElseThrow(() -> new BusinessException(ErrorCode.KLASS_NOT_FOUND));
    }

    private void validateOwnership(Klass klass, Long creatorId) {
        if (!klass.isOwnedBy(creatorId)) throw new BusinessException(ErrorCode.KLASS_ACCESS_DENIED);
    }
}
