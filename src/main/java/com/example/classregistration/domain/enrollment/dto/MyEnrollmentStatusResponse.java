package com.example.classregistration.domain.enrollment.dto;

import com.example.classregistration.domain.enrollment.model.EnrollmentStatus;

public record MyEnrollmentStatusResponse(
        boolean enrolled,
        Long enrollmentId,
        EnrollmentStatus status
) {}
