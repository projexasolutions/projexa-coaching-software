package com.projexa.coaching.academics.repository;

import com.projexa.coaching.academics.entity.AcademicClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AcademicClassRepository extends JpaRepository<AcademicClass, UUID> {
    List<AcademicClass> findAllByTenantId(UUID tenantId);
    Optional<AcademicClass> findByIdAndTenantId(UUID id, UUID tenantId);
}