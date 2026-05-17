package com.example.classregistration.domain.klass.dto;

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
    ) {}
}
