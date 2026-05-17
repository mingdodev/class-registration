package com.example.classregistration.domain.klass.dto;

import java.time.LocalDate;

public record UpdateKlassRequest(
        String title,
        String description,
        Integer price,
        Integer maxCapacity,
        LocalDate startDate,
        LocalDate endDate
) {}
