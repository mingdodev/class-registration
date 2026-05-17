package com.example.classregistration.domain.waitlist.dto;

import java.time.LocalDateTime;

public record MyWaitlistStatusResponse(
        boolean registered,
        LocalDateTime createdAt
) {}
