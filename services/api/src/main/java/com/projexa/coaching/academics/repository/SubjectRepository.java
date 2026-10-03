package com.projexa.coaching.academics.repository;

import com.projexa.coaching.academics.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubjectRepository extends JpaRepository<Subject, UUID> {
    List<Subject> findAllByTenantId(UUID tenantId);
    Optional<Subject> findByIdAndTenantId(UUID id, UUID tenantId);
}