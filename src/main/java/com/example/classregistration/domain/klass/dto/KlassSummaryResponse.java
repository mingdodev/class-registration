package com.example.classregistration.domain.klass.dto;

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
) {}
