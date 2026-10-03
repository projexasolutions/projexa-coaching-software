package com.projexa.coaching.students.repository;

import com.projexa.coaching.students.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentRepository extends JpaRepository<Student, UUID> {
    List<Student> findAllByTenantId(UUID tenantId);
    Optional<Student> findByIdAndTenantId(UUID id, UUID tenantId);
    boolean existsByTenantIdAndAdmissionNumber(UUID tenantId, String admissionNumber);
    boolean existsByTenantIdAndAdmissionNumberAndIdNot(UUID tenantId, String admissionNumber, UUID id);
}
