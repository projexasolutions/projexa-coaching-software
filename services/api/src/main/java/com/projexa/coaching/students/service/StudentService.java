package com.projexa.coaching.students.service;

import com.projexa.coaching.common.exceptions.ApiException;
import com.projexa.coaching.common.tenant.TenantContextHolder;
import com.projexa.coaching.students.entity.Student;
import com.projexa.coaching.students.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class StudentService {
    private static final Set<String> ALLOWED_STATUS = Set.of("ACTIVE", "INACTIVE", "WITHDRAWN", "TRANSFERRED", "ALUMNI");

    private final StudentRepository repo;

    public StudentService(StudentRepository repo) {
        this.repo = repo;
    }

    public List<Student> list() {
        return repo.findAllByTenantId(TenantContextHolder.getRequired());
    }

    public Student get(UUID id) {
        return repo.findByIdAndTenantId(id, TenantContextHolder.getRequired())
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Student not found"));
    }

    public Student create(Student item) {
        UUID tenantId = TenantContextHolder.getRequired();
        normalizeAndValidate(item);
        if (repo.existsByTenantIdAndAdmissionNumber(tenantId, item.getAdmissionNumber())) {
            throw new ApiException("DUPLICATE_ADMISSION_NUMBER", "Admission number already exists");
        }
        item.setTenantId(tenantId);
        return repo.save(item);
    }

    public Student update(UUID id, Student item) {
        UUID tenantId = TenantContextHolder.getRequired();
        Student current = get(id);
        normalizeAndValidate(item);

        if (repo.existsByTenantIdAndAdmissionNumberAndIdNot(tenantId, item.getAdmissionNumber(), id)) {
            throw new ApiException("DUPLICATE_ADMISSION_NUMBER", "Admission number already exists");
        }

        copy(item, current);
        return repo.save(current);
    }

    public void delete(UUID id) {
        Student current = get(id);
        current.setStatus("INACTIVE");
        repo.save(current);
    }

    private void normalizeAndValidate(Student s) {
        if (s == null) {
            throw new ApiException("VALIDATION_ERROR", "Student payload is required");
        }

        s.setAdmissionNumber(trimRequired(s.getAdmissionNumber(), "Admission number"));
        s.setFirstName(trimRequired(s.getFirstName(), "First name"));
        s.setLastName(trimNullable(s.getLastName()));
        s.setEmail(trimNullable(s.getEmail()));
        s.setPhone(trimNullable(s.getPhone()));
        s.setGender(trimNullable(s.getGender()));
        s.setAddress(trimNullable(s.getAddress()));
        s.setProfilePhotoUrl(trimNullable(s.getProfilePhotoUrl()));

        if (s.getDateOfBirth() != null && s.getDateOfBirth().isAfter(LocalDate.now())) {
            throw new ApiException("VALIDATION_ERROR", "Date of birth cannot be in the future");
        }

        String status = trimNullable(s.getStatus());
        if (status == null) {
            status = "ACTIVE";
        }
        status = status.toUpperCase();
        if (!ALLOWED_STATUS.contains(status)) {
            throw new ApiException("VALIDATION_ERROR", "Unsupported student status");
        }
        s.setStatus(status);
    }

    private String trimRequired(String value, String field) {
        String result = trimNullable(value);
        if (result == null) {
            throw new ApiException("VALIDATION_ERROR", field + " is required");
        }
        return result;
    }

    private String trimNullable(String value) {
        if (value == null) return null;
        String result = value.trim();
        return result.isEmpty() ? null : result;
    }

    private void copy(Student source, Student target) {
        target.setAdmissionNumber(source.getAdmissionNumber());
        target.setFirstName(source.getFirstName());
        target.setLastName(source.getLastName());
        target.setEmail(source.getEmail());
        target.setPhone(source.getPhone());
        target.setDateOfBirth(source.getDateOfBirth());
        target.setGender(source.getGender());
        target.setAddress(source.getAddress());
        target.setProfilePhotoUrl(source.getProfilePhotoUrl());
        target.setStatus(source.getStatus());
    }
}
