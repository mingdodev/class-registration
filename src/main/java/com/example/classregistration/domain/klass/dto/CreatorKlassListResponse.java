package com.example.classregistration.domain.klass.dto;

import com.example.classregistration.domain.klass.model.KlassStatus;

import java.time.LocalDate;
import java.util.List;

public record CreatorKlassListResponse(
        List<KlassSummary> klasses
) {
    public record KlassSummary(
            Long id,
            String title,
            int price,
            KlassStatus status,
            int remainingCapacity,
            LocalDate startDate,
            LocalDate endDate
    ) {}
}
