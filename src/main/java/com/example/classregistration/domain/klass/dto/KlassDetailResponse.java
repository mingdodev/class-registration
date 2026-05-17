package com.example.classregistration.domain.klass.dto;

import com.example.classregistration.domain.klass.model.KlassStatus;

import java.time.LocalDate;

public record KlassDetailResponse(
        Long id,
        String title,
        String description,
        CreatorInfo creator,
        int price,
        KlassStatus status,
        int maxCapacity,
        int remainingCapacity,
        int enrolledCount,
        LocalDate startDate,
        LocalDate endDate
) {
    public record CreatorInfo(Long id, String name) {}
}
