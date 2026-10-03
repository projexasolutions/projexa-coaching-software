package com.projexa.coaching.academics.repository.StreamRepository;
import com.projexa.coaching.academics.repository.entity.Stream; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface StreamRepository extends JpaRepository<Stream,UUID> { List<Stream> findAllByTenantId(UUID tenantId); Optional<Stream> findByIdAndTenantId(UUID id,UUID tenantId); }