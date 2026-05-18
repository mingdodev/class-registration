package com.example.classregistration.domain.creator;

import com.example.classregistration.domain.creator.model.Creator;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreatorRepository extends JpaRepository<Creator, Long> {
}
