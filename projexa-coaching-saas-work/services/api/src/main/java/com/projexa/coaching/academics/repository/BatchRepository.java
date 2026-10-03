package com.projexa.coaching.academics.repository.BatchRepository;
import com.projexa.coaching.academics.repository.entity.Batch; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface BatchRepository extends JpaRepository<Batch,UUID> { List<Batch> findAllByTenantId(UUID tenantId); Optional<Batch> findByIdAndTenantId(UUID id,UUID tenantId); }