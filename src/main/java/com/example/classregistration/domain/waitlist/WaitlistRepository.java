package com.example.classregistration.domain.waitlist;

import com.example.classregistration.domain.waitlist.model.Waitlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WaitlistRepository extends JpaRepository<Waitlist, Long> {

    boolean existsByKlassmateIdAndKlassId(Long klassmateId, Long klassId);

    Optional<Waitlist> findByKlassmateIdAndKlassId(Long klassmateId, Long klassId);

    @Query("SELECT w FROM Waitlist w WHERE w.klass.id = :klassId ORDER BY w.createdAt ASC LIMIT 1")
    Optional<Waitlist> findFirstWaiterByKlassId(@Param("klassId") Long klassId);
}
