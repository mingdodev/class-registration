package com.example.classregistration.domain.klass;

import com.example.classregistration.domain.enrollment.model.Enrollment;
import com.example.classregistration.domain.klass.dto.*;
import com.example.classregistration.domain.klass.model.Klass;
import com.example.classregistration.domain.klass.model.KlassStatus;
import com.example.classregistration.global.response.ApiResponse;
import com.example.classregistration.global.response.CursorPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class KlassController {

    private final KlassService klassService;

    @GetMapping("/api/klasses")
    public ApiResponse<CursorPage<KlassSummaryResponse>> getKlasses(
            @RequestParam(required = false) KlassStatus status,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(klassService.getKlasses(status, cursor, size));
    }

    @GetMapping("/api/klasses/{klassId}")
    public ApiResponse<KlassDetailResponse> getKlass(@PathVariable Long klassId) {
        return ApiResponse.ok(KlassDetailResponse.from(klassService.getKlass(klassId)));
    }

    @PostMapping("/api/klasses")
    public ResponseEntity<ApiResponse<Map<String, Long>>> createKlass(
            @RequestHeader("X-Creator-Id") Long creatorId,
            @RequestBody @Valid CreateKlassRequest request) {
        Long id = klassService.createKlass(creatorId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(Map.of("id", id)));
    }

    @PatchMapping("/api/klasses/{klassId}/open")
    public ApiResponse<Void> openKlass(
            @RequestHeader("X-Creator-Id") Long creatorId,
            @PathVariable Long klassId) {
        klassService.openKlass(creatorId, klassId);
        return ApiResponse.ok();
    }

    @PatchMapping("/api/klasses/{klassId}")
    public ApiResponse<Void> updateKlass(
            @RequestHeader("X-Creator-Id") Long creatorId,
            @PathVariable Long klassId,
            @RequestBody UpdateKlassRequest request) {
        klassService.updateKlass(creatorId, klassId, request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/api/klasses/{klassId}")
    public ApiResponse<Void> deleteKlass(
            @RequestHeader("X-Creator-Id") Long creatorId,
            @PathVariable Long klassId) {
        klassService.deleteKlass(creatorId, klassId);
        return ApiResponse.ok();
    }

    @GetMapping("/api/creators/me/klasses")
    public ApiResponse<CreatorKlassListResponse> getCreatorKlasses(
            @RequestHeader("X-Creator-Id") Long creatorId,
            @RequestParam(required = false) KlassStatus status) {
        List<Klass> klasses = klassService.getCreatorKlasses(creatorId, status);
        return ApiResponse.ok(CreatorKlassListResponse.from(klasses));
    }

    @GetMapping("/api/klasses/{klassId}/klassmates")
    public ApiResponse<KlassmatesResponse> getKlassmates(
            @RequestHeader("X-Creator-Id") Long creatorId,
            @PathVariable Long klassId) {
        List<Enrollment> enrollments = klassService.getKlassmates(creatorId, klassId);
        return ApiResponse.ok(KlassmatesResponse.from(enrollments));
    }
}
