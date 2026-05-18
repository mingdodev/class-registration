package com.example.classregistration.domain.waitlist.controller;

import com.example.classregistration.domain.waitlist.dto.MyWaitlistStatusResponse;
import com.example.classregistration.domain.waitlist.service.WaitlistService;
import com.example.classregistration.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class WaitlistController {

    private final WaitlistService waitlistService;

    @PostMapping("/api/klasses/{klassId}/waitlist")
    public ResponseEntity<ApiResponse<Void>> joinWaitlist(
            @RequestHeader("X-Klassmate-Id") Long klassmateId,
            @PathVariable Long klassId) {
        waitlistService.joinWaitlist(klassmateId, klassId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok());
    }

    @GetMapping("/api/klasses/{klassId}/waitlist/me")
    public ApiResponse<MyWaitlistStatusResponse> getMyWaitlistStatus(
            @RequestHeader("X-Klassmate-Id") Long klassmateId,
            @PathVariable Long klassId) {
        return ApiResponse.ok(waitlistService.getMyWaitlistStatus(klassmateId, klassId));
    }

    @DeleteMapping("/api/klasses/{klassId}/waitlist")
    public ApiResponse<Void> leaveWaitlist(
            @RequestHeader("X-Klassmate-Id") Long klassmateId,
            @PathVariable Long klassId) {
        waitlistService.leaveWaitlist(klassmateId, klassId);
        return ApiResponse.ok();
    }
}
