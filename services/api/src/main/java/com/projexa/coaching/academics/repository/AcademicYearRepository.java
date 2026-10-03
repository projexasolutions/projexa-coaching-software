package com.projexa.coaching.academics.repository;

import com.projexa.coaching.academics.entity.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AcademicYearRepository extends JpaRepository<AcademicYear, UUID> {
    List<AcademicYear> findAllByTenantId(UUID tenantId);
    Optional<AcademicYear> findByIdAndTenantId(UUID id, UUID tenantId);
}