package com.example.classregistration.domain.waitlist;

import com.example.classregistration.domain.waitlist.model.Waitlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WaitlistRepository extends JpaRepository<Waitlist, Long> {

    boolean existsByKlassmateIdAndKlassId(Long klassmateId, Long klassId);

    Optional<Waitlist> findByKlassmateIdAndKlassId(Long klassmateId, Long klassId);
}
