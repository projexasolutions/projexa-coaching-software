package com.projexa.coaching.academics.repository;

import com.projexa.coaching.academics.entity.Batch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BatchRepository extends JpaRepository<Batch, UUID> {
    List<Batch> findAllByTenantId(UUID tenantId);
    Optional<Batch> findByIdAndTenantId(UUID id, UUID tenantId);
}