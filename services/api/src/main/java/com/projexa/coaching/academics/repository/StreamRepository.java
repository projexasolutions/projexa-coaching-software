package com.projexa.coaching.academics.repository;

import com.projexa.coaching.academics.entity.Stream;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StreamRepository extends JpaRepository<Stream, UUID> {
    List<Stream> findAllByTenantId(UUID tenantId);
    Optional<Stream> findByIdAndTenantId(UUID id, UUID tenantId);
}