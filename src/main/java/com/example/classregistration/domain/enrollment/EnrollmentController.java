package com.example.classregistration.domain.enrollment;

import com.example.classregistration.domain.enrollment.dto.CreateEnrollmentResponse;
import com.example.classregistration.domain.enrollment.dto.MyEnrollmentResponse;
import com.example.classregistration.domain.enrollment.dto.MyEnrollmentStatusResponse;
import com.example.classregistration.domain.enrollment.model.EnrollmentStatus;
import com.example.classregistration.global.response.ApiResponse;
import com.example.classregistration.global.response.CursorPage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping("/api/klasses/{klassId}/enrollments")
    public ResponseEntity<ApiResponse<CreateEnrollmentResponse>> enroll(
            @RequestHeader("X-Klassmate-Id") Long klassmateId,
            @PathVariable Long klassId) {
        CreateEnrollmentResponse response = enrollmentService.enroll(klassmateId, klassId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @PostMapping("/api/enrollments/{enrollmentId}/confirm")
    public ApiResponse<Void> confirmEnrollment(
            @RequestHeader("X-Klassmate-Id") Long klassmateId,
            @PathVariable Long enrollmentId) {
        enrollmentService.confirmEnrollment(klassmateId, enrollmentId);
        return ApiResponse.ok();
    }

    @DeleteMapping("/api/enrollments/{enrollmentId}")
    public ApiResponse<Void> cancelEnrollment(
            @RequestHeader("X-Klassmate-Id") Long klassmateId,
            @PathVariable Long enrollmentId) {
        enrollmentService.cancelEnrollment(klassmateId, enrollmentId);
        return ApiResponse.ok();
    }

    @GetMapping("/api/klassmates/me/enrollments")
    public ApiResponse<CursorPage<MyEnrollmentResponse>> getMyEnrollments(
            @RequestHeader("X-Klassmate-Id") Long klassmateId,
            @RequestParam(required = false) EnrollmentStatus status,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(enrollmentService.getMyEnrollments(klassmateId, status, cursor, size));
    }

    @GetMapping("/api/klasses/{klassId}/enrollments/me")
    public ApiResponse<MyEnrollmentStatusResponse> getMyEnrollmentStatus(
            @RequestHeader("X-Klassmate-Id") Long klassmateId,
            @PathVariable Long klassId) {
        return ApiResponse.ok(enrollmentService.getMyEnrollmentStatus(klassmateId, klassId));
    }
}
