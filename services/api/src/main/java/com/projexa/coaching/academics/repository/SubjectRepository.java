package com.projexa.coaching.academics.repository.SubjectRepository;
import com.projexa.coaching.academics.repository.entity.Subject; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface SubjectRepository extends JpaRepository<Subject,UUID> { List<Subject> findAllByTenantId(UUID tenantId); Optional<Subject> findByIdAndTenantId(UUID id,UUID tenantId); }