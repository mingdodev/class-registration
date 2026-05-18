package com.example.classregistration.domain.klassmate;

import com.example.classregistration.domain.klassmate.model.Klassmate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KlassmateRepository extends JpaRepository<Klassmate, Long> {
}
