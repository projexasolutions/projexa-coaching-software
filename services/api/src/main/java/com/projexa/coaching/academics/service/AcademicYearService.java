package com.projexa.coaching.academics.service;

import com.projexa.coaching.common.exceptions.ApiException;
import com.projexa.coaching.common.tenant.TenantContextHolder;
import com.projexa.coaching.academics.entity.AcademicYear;
import com.projexa.coaching.academics.repository.AcademicYearRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class AcademicYearService {
    private final AcademicYearRepository repo;

    public AcademicYearService(AcademicYearRepository repo) {
        this.repo = repo;
    }

    public List<AcademicYear> list() {
        return repo.findAllByTenantId(TenantContextHolder.getRequired());
    }

    public AcademicYear get(UUID id) {
        return repo.findByIdAndTenantId(id, TenantContextHolder.getRequired())
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Academic year not found"));
    }

    public AcademicYear create(AcademicYear item) {
        UUID tenantId = TenantContextHolder.getRequired();
        validate(item);
        item.setTenantId(tenantId);
        return repo.save(item);
    }

    public AcademicYear update(UUID id, AcademicYear item) {
        AcademicYear current = get(id);
        validate(item);
        copy(item, current);
        return repo.save(current);
    }

    public void delete(UUID id) {
        AcademicYear current = get(id);
        current.setStatus("ARCHIVED");
        current.setCurrent(false);
        repo.save(current);
    }

    private void validate(AcademicYear item) {
        if (item == null) throw new ApiException("VALIDATION_ERROR", "Academic year payload is required");
        if (item.getName() == null || item.getName().trim().isEmpty()) throw new ApiException("VALIDATION_ERROR", "Academic year name is required");
        if (item.getStartDate() == null || item.getEndDate() == null) throw new ApiException("VALIDATION_ERROR", "Start and end dates are required");
        if (item.getEndDate().isBefore(item.getStartDate())) throw new ApiException("VALIDATION_ERROR", "End date cannot be before start date");
        item.setName(item.getName().trim());
        if (item.getStartDate().isBefore(LocalDate.of(2000,1,1))) throw new ApiException("VALIDATION_ERROR", "Invalid start date");
        String status=item.getStatus()==null?"ACTIVE":item.getStatus().trim().toUpperCase();
        if (!status.equals("ACTIVE") && !status.equals("ARCHIVED")) throw new ApiException("VALIDATION_ERROR", "Unsupported academic year status");
        item.setStatus(status);
    }

    private void copy(AcademicYear s, AcademicYear t) {
        t.setName(s.getName());
        t.setStartDate(s.getStartDate());
        t.setEndDate(s.getEndDate());
        t.setCurrent(s.isCurrent());
        t.setStatus(s.getStatus());
    }
}
