package com.example.classregistration.domain.klass.dto;

import com.example.classregistration.domain.klass.model.Klass;
import com.example.classregistration.domain.klass.model.KlassStatus;

import java.time.LocalDate;

public record KlassSummaryResponse(
        Long id,
        String title,
        int price,
        KlassStatus status,
        int remainingCapacity,
        LocalDate startDate,
        LocalDate endDate
) {
    public static KlassSummaryResponse from(Klass klass) {
        return new KlassSummaryResponse(
                klass.getId(),
                klass.getTitle(),
                klass.getPrice(),
                klass.getStatus(),
                klass.getRemainingCapacity(),
                klass.getStartDate(),
                klass.getEndDate());
    }
}
