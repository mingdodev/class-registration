package com.example.classregistration.domain.klass.dto;

import com.example.classregistration.domain.enrollment.model.Enrollment;
import com.example.classregistration.domain.enrollment.model.EnrollmentStatus;

import java.time.LocalDateTime;
import java.util.List;

public record KlassmatesResponse(
        List<KlassmateInfo> klassmates
) {
    public record KlassmateInfo(
            Long id,
            String name,
            String email,
            String phoneNumber,
            EnrollmentStatus enrollmentStatus,
            LocalDateTime enrolledAt
    ) {
        public static KlassmateInfo from(Enrollment enrollment) {
            return new KlassmateInfo(
                    enrollment.getKlassmate().getId(),
                    enrollment.getKlassmate().getName(),
                    enrollment.getKlassmate().getEmail(),
                    enrollment.getKlassmate().getPhoneNumber(),
                    enrollment.getStatus(),
                    enrollment.getCreatedAt());
        }
    }

    public static KlassmatesResponse from(List<Enrollment> enrollments) {
        return new KlassmatesResponse(
                enrollments.stream().map(KlassmateInfo::from).toList());
    }
}
