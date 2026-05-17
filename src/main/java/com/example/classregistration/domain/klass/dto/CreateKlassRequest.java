package com.example.classregistration.domain.klass.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateKlassRequest(
        @NotBlank String title,
        String description,
        @NotNull Integer price,
        @NotNull Integer maxCapacity,
        LocalDate startDate,
        LocalDate endDate
) {}
