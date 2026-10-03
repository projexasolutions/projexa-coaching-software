package com.projexa.coaching.academics.service;

import com.projexa.coaching.academics.entity.Batch;
import com.projexa.coaching.academics.repository.BatchRepository;
import com.projexa.coaching.common.exceptions.ApiException;
import com.projexa.coaching.common.tenant.TenantContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class BatchService {
    private final BatchRepository repo;
    private final JdbcTemplate db;

    public BatchService(BatchRepository repo, JdbcTemplate db) {
        this.repo = repo;
        this.db = db;
    }

    public List<Batch> list() {
        return repo.findAllByTenantId(TenantContextHolder.getRequired());
    }

    public Batch get(UUID id) {
        return repo.findByIdAndTenantId(id, TenantContextHolder.getRequired())
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Batch not found"));
    }

    public Batch create(Batch item) {
        UUID tenantId = TenantContextHolder.getRequired();
        validate(item, tenantId, null);
        item.setTenantId(tenantId);
        return repo.save(item);
    }

    public Batch update(UUID id, Batch item) {
        UUID tenantId = TenantContextHolder.getRequired();
        Batch current = get(id);
        validate(item, tenantId, id);
        copy(item, current);
        return repo.save(current);
    }

    public void delete(UUID id) {
        repo.delete(get(id));
    }

    private void validate(Batch item, UUID tenantId, UUID currentId) {
        if (item == null) throw new ApiException("VALIDATION_ERROR", "Batch payload is required");
        if (item.getAcademicYearId() == null) throw new ApiException("VALIDATION_ERROR", "Academic year is required");
        if (item.getClassId() == null) throw new ApiException("VALIDATION_ERROR", "Class is required");

        String name = item.getName() == null ? "" : item.getName().trim();
        if (name.isEmpty()) throw new ApiException("VALIDATION_ERROR", "Batch name is required");
        item.setName(name);

        if (item.getCode() != null) {
            String code = item.getCode().trim();
            item.setCode(code.isEmpty() ? null : code);
        }

        if (item.getCapacity() != null && item.getCapacity() < 0) {
            throw new ApiException("VALIDATION_ERROR", "Capacity cannot be negative");
        }

        String status = item.getStatus() == null ? "" : item.getStatus().trim().toUpperCase();
        if (status.isEmpty()) status = "ACTIVE";
        if (!status.equals("ACTIVE") && !status.equals("INACTIVE")) {
            throw new ApiException("VALIDATION_ERROR", "Unsupported batch status");
        }
        item.setStatus(status);

        require("select count(*) from academic_years where id=? and tenant_id=?", item.getAcademicYearId(), tenantId, "Academic year");
        require("select count(*) from classes where id=? and tenant_id=?", item.getClassId(), tenantId, "Class");
        if (item.getStreamId() != null) {
            require("select count(*) from streams where id=? and tenant_id=?", item.getStreamId(), tenantId, "Stream");
        }

        boolean duplicate = currentId == null
                ? repo.existsByTenantIdAndAcademicYearIdAndName(tenantId, item.getAcademicYearId(), name)
                : repo.existsByTenantIdAndAcademicYearIdAndNameAndIdNot(tenantId, item.getAcademicYearId(), name, currentId);

        if (duplicate) {
            throw new ApiException("DUPLICATE_BATCH_NAME", "A batch with this name already exists in the academic year");
        }
    }

    private void require(String sql, UUID id, UUID tenantId, String label) {
        Integer count = db.queryForObject(sql, Integer.class, id, tenantId);
        if (count == null || count == 0) throw new ApiException("NOT_FOUND", label + " not found");
    }

    private void copy(Batch source, Batch target) {
        target.setAcademicYearId(source.getAcademicYearId());
        target.setClassId(source.getClassId());
        target.setStreamId(source.getStreamId());
        target.setName(source.getName());
        target.setCode(source.getCode());
        target.setCapacity(source.getCapacity());
        target.setStatus(source.getStatus());
    }
}
