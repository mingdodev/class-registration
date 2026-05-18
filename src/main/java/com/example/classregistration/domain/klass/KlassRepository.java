package com.example.classregistration.domain.klass;

import com.example.classregistration.domain.klass.model.Klass;
import com.example.classregistration.domain.klass.model.KlassStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KlassRepository extends JpaRepository<Klass, Long> {

    List<Klass> findByCreatorId(Long creatorId);

    List<Klass> findByCreatorIdAndStatus(Long creatorId, KlassStatus status);

    // ADR-04: 원자적 업데이트로 정원 차감. 0행 반환 시 정원 초과
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Klass k SET k.remainingCapacity = k.remainingCapacity - 1 WHERE k.id = :klassId AND k.remainingCapacity > 0")
    int decreaseRemainingCapacity(@Param("klassId") Long klassId);

    // ADR-04: 수강 취소 시 정원 복구
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Klass k SET k.remainingCapacity = k.remainingCapacity + 1 WHERE k.id = :klassId")
    void increaseRemainingCapacity(@Param("klassId") Long klassId);
}
