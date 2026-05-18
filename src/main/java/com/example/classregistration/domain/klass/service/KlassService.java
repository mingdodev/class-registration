package com.example.classregistration.domain.klass.service;

import com.example.classregistration.domain.creator.repository.CreatorRepository;
import com.example.classregistration.domain.creator.model.Creator;
import com.example.classregistration.domain.enrollment.repository.EnrollmentRepository;
import com.example.classregistration.domain.enrollment.model.Enrollment;
import com.example.classregistration.domain.klass.dto.CreateKlassRequest;
import com.example.classregistration.domain.klass.dto.UpdateKlassRequest;
import com.example.classregistration.domain.klass.model.Klass;
import com.example.classregistration.domain.klass.model.KlassStatus;
import com.example.classregistration.domain.klass.repository.KlassRepository;
import com.example.classregistration.domain.waitlist.publisher.WaitlistEventPublisher;
import com.example.classregistration.global.exception.BusinessException;
import com.example.classregistration.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.classregistration.domain.klass.dto.KlassSummaryResponse;
import com.example.classregistration.global.response.CursorPage;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    public CursorPage<KlassSummaryResponse> getKlasses(KlassStatus status, String cursor, int size) {
        LocalDateTime cursorTime = cursor != null ? LocalDateTime.parse(cursor) : null;
        List<Klass> klasses = klassRepository.findPublicKlasses(status, cursorTime, PageRequest.of(0, size + 1));
        boolean hasNext = klasses.size() > size;
        List<Klass> content = hasNext ? klasses.subList(0, size) : klasses;
        String nextCursor = hasNext ? content.get(content.size() - 1).getCreatedAt().toString() : null;
        return new CursorPage<>(content.stream().map(KlassSummaryResponse::from).toList(), nextCursor, hasNext);
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
