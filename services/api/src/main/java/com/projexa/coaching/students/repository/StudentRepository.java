package com.projexa.coaching.students.repository.StudentRepository;
import com.projexa.coaching.students.repository.entity.Student; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface StudentRepository extends JpaRepository<Student,UUID> { List<Student> findAllByTenantId(UUID tenantId); Optional<Student> findByIdAndTenantId(UUID id,UUID tenantId); }