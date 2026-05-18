package com.example.classregistration.domain.klass;

import com.example.classregistration.domain.klass.model.Klass;
import com.example.classregistration.domain.klass.model.KlassStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface KlassRepository extends JpaRepository<Klass, Long> {

    List<Klass> findByCreatorId(Long creatorId);

    List<Klass> findByCreatorIdAndStatus(Long creatorId, KlassStatus status);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Klass k SET k.remainingCapacity = k.remainingCapacity - 1 WHERE k.id = :klassId AND k.remainingCapacity > 0")
    int decreaseRemainingCapacity(@Param("klassId") Long klassId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Klass k SET k.remainingCapacity = k.remainingCapacity + 1 WHERE k.id = :klassId")
    void increaseRemainingCapacity(@Param("klassId") Long klassId);

    // 스케줄러: 수강 종료일이 지난 OPEN 강의 조회
    @Query("SELECT k FROM Klass k WHERE k.status = 'OPEN' AND k.endDate IS NOT NULL AND k.endDate < :today")
    List<Klass> findExpiredOpenKlasses(@Param("today") LocalDate today);

    // 전체 강의 목록 조회 (OPEN/CLOSED, 커서 페이지네이션)
    @Query("SELECT k FROM Klass k WHERE k.status IN ('OPEN', 'CLOSED') AND (:status IS NULL OR k.status = :status) AND (:cursor IS NULL OR k.createdAt < :cursor) ORDER BY k.createdAt DESC")
    List<Klass> findPublicKlasses(@Param("status") KlassStatus status, @Param("cursor") LocalDateTime cursor, Pageable pageable);
}
