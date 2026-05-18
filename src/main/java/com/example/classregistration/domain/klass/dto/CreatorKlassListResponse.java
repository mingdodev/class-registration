package com.example.classregistration.domain.klass.dto;

import com.example.classregistration.domain.klass.model.Klass;
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
    ) {
        public static KlassSummary from(Klass klass) {
            return new KlassSummary(
                    klass.getId(),
                    klass.getTitle(),
                    klass.getPrice(),
                    klass.getStatus(),
                    klass.getRemainingCapacity(),
                    klass.getStartDate(),
                    klass.getEndDate());
        }
    }

    public static CreatorKlassListResponse from(List<Klass> klasses) {
        return new CreatorKlassListResponse(
                klasses.stream().map(KlassSummary::from).toList());
    }
}
