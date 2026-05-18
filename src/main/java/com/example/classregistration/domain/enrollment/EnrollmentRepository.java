package com.example.classregistration.domain.enrollment;

import com.example.classregistration.domain.enrollment.model.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    Optional<Enrollment> findByIdAndKlassmateId(Long id, Long klassmateId);

    // 취소되지 않은 수강신청 존재 여부 확인 (중복 신청 방지)
    @Query("SELECT COUNT(e) > 0 FROM Enrollment e WHERE e.klassmate.id = :klassmateId AND e.klass.id = :klassId AND e.status <> 'CANCELLED'")
    boolean isAlreadyEnrolled(@Param("klassmateId") Long klassmateId, @Param("klassId") Long klassId);

    // 강의별 등록된(PENDING 또는 CONFIRMED) 수강신청 목록 조회
    @Query("SELECT e FROM Enrollment e WHERE e.klass.id = :klassId AND e.status IN ('PENDING', 'CONFIRMED')")
    List<Enrollment> findRegisteredEnrollmentsByKlassId(@Param("klassId") Long klassId);
}
