package com.example.classregistration.domain.enrollment.dto;

import com.example.classregistration.domain.enrollment.model.CancelReason;
import com.example.classregistration.domain.enrollment.model.Enrollment;
import com.example.classregistration.domain.enrollment.model.EnrollmentStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MyEnrollmentResponse(
        Long id,
        KlassInfo klass,
        EnrollmentStatus status,
        CancelReason cancelReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record KlassInfo(Long id, String title, LocalDate startDate, LocalDate endDate) {}

    public static MyEnrollmentResponse from(Enrollment enrollment) {
        var klass = enrollment.getKlass();
        return new MyEnrollmentResponse(
                enrollment.getId(),
                new KlassInfo(klass.getId(), klass.getTitle(), klass.getStartDate(), klass.getEndDate()),
                enrollment.getStatus(),
                enrollment.getCancelReason(),
                enrollment.getCreatedAt(),
                enrollment.getUpdatedAt()
        );
    }
}
