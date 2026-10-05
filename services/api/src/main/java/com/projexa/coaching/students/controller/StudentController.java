package com.projexa.coaching.students.controller;

import com.projexa.coaching.common.exceptions.ApiException;
import com.projexa.coaching.common.responses.ApiResponse;
import com.projexa.coaching.common.tenant.TenantContextHolder;
import com.projexa.coaching.students.entity.Student;
import com.projexa.coaching.students.service.StudentService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/students")
public class StudentController {
    private final StudentService service;
    private final JdbcTemplate db;

    public StudentController(StudentService service, JdbcTemplate db) {
        this.service = service;
        this.db = db;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('dashboard.read') or hasAuthority('settings.manage') or hasAuthority('students.read')")
    public ApiResponse<List<Student>> list() {
        return ApiResponse.ok(service.list());
    }

    @GetMapping("/operational")
    @PreAuthorize("hasAuthority('dashboard.read') or hasAuthority('students.read') or hasAuthority('settings.manage')")
    public List<Map<String, Object>> operational() {
        UUID tenantId = TenantContextHolder.getRequired();
        return db.queryForList("""
                select
                    s.id,
                    s.tenant_id as "tenantId",
                    s.admission_number as "admissionNumber",
                    s.first_name as "firstName",
                    s.last_name as "lastName",
                    s.email,
                    s.phone,
                    s.date_of_birth as "dateOfBirth",
                    s.gender,
                    s.status,
                    s.address,
                    s.profile_photo_url as "profilePhotoUrl",
                    s.created_at as "createdAt",
                    s.updated_at as "updatedAt",
                    e.id as "enrollmentId",
                    e.program_id as "programId",
                    e.batch_id as "batchId",
                    e.class_id as "classId",
                    e.stream_id as "streamId",
                    e.academic_year_id as "academicYearId",
                    b.name as "batchName",
                    c.name as "className",
                    st.name as "streamName",
                    ay.name as "academicYear",
                    p.name as "programName"
                from students s
                left join enrollments e
                    on e.student_id=s.id
                   and e.tenant_id=s.tenant_id
                   and e.status='ACTIVE'
                left join batches b on b.id=e.batch_id and b.tenant_id=s.tenant_id
                left join classes c on c.id=e.class_id and c.tenant_id=s.tenant_id
                left join streams st on st.id=e.stream_id and st.tenant_id=s.tenant_id
                left join academic_years ay on ay.id=e.academic_year_id and ay.tenant_id=s.tenant_id
                left join programs p on p.id=e.program_id and p.tenant_id=s.tenant_id
                where s.tenant_id=?
                order by s.created_at desc
                """, tenantId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('students.read') or hasAnyRole('INSTITUTE_OWNER','INSTITUTE_ADMIN')")
    public ApiResponse<Student> get(@PathVariable UUID id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('settings.manage') or hasAuthority('students.create')")
    public ApiResponse<Student> create(@RequestBody Student item) {
        return ApiResponse.ok(service.create(item));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('settings.manage') or hasAuthority('students.update')")
    public ApiResponse<Student> update(@PathVariable UUID id, @RequestBody Student item) {
        return ApiResponse.ok(service.update(id, item));
    }

    @PostMapping("/{id}/enrollment")
    @Transactional
    @PreAuthorize("hasAuthority('settings.manage') or hasAuthority('students.update')")
    public Map<String, Object> enroll(@PathVariable UUID id, @RequestBody EnrollmentRequest req) {
        UUID tenantId = TenantContextHolder.getRequired();
        requireTenantRow("students", id, tenantId, "Student");
        if (req == null) {
            throw new ApiException("VALIDATION_ERROR", "Enrollment details are required");
        }

        requireTenantRow("academic_years", req.academicYearId(), tenantId, "Academic year");
        requireTenantRow("classes", req.classId(), tenantId, "Class");
        requireTenantRow("batches", req.batchId(), tenantId, "Batch");
        if (req.streamId() != null) {
            requireTenantRow("streams", req.streamId(), tenantId, "Stream");
        }
        if (req.programId() != null) {
            requireTenantRow("programs", req.programId(), tenantId, "Program");
        }

        Map<String, Object> batch = db.queryForMap("""
                select class_id,stream_id,academic_year_id,program_id,capacity,status
                from batches
                where id=? and tenant_id=?
                for update
                """, req.batchId(), tenantId);

        if (!"ACTIVE".equalsIgnoreCase(String.valueOf(batch.get("status")))) {
            throw new ApiException("VALIDATION_ERROR", "Selected batch is not active");
        }
        if (!req.classId().equals(batch.get("class_id"))) {
            throw new ApiException("VALIDATION_ERROR", "Selected class does not match the batch");
        }

        Object batchStreamId = batch.get("stream_id");
        if (batchStreamId == null ? req.streamId() != null : !batchStreamId.equals(req.streamId())) {
            throw new ApiException("VALIDATION_ERROR", "Selected stream does not match the batch");
        }

        if (!req.academicYearId().equals(batch.get("academic_year_id"))) {
            throw new ApiException("VALIDATION_ERROR", "Selected academic year does not match the batch");
        }

        UUID effectiveProgramId = req.programId();
        Object batchProgramId = batch.get("program_id");
        if (effectiveProgramId == null && batchProgramId instanceof UUID) {
            effectiveProgramId = (UUID) batchProgramId;
        } else if (effectiveProgramId != null && batchProgramId != null && !effectiveProgramId.equals(batchProgramId)) {
            throw new ApiException("VALIDATION_ERROR", "Selected program does not match the batch");
        }

        if (req.streamId() != null) {
            int mapped = count("""
                    select count(*) from class_streams
                    where tenant_id=? and academic_year_id=? and class_id=? and stream_id=?
                    """, tenantId, req.academicYearId(), req.classId(), req.streamId());
            if (mapped == 0) {
                throw new ApiException("VALIDATION_ERROR", "Selected stream is not configured for this class and academic year");
            }
        }

        if (effectiveProgramId != null) {
            int active = count("""
                    select count(*) from program_subjects
                    where tenant_id=? and program_id=? and active=true
                    """, tenantId, effectiveProgramId);
            if (active == 0) {
                throw new ApiException("VALIDATION_ERROR", "Selected program has no active subjects configured");
            }
        }

        int capacity = batch.get("capacity") == null ? 0 : ((Number) batch.get("capacity")).intValue();
        if (capacity > 0) {
            int existing = count("""
                    select count(*) from enrollments
                    where tenant_id=? and batch_id=? and status='ACTIVE' and student_id<>?
                    """, tenantId, req.batchId(), id);
            if (existing >= capacity) {
                throw new ApiException("BATCH_CAPACITY_REACHED", "Selected batch has reached its capacity");
            }
        }

        db.update("""
                update enrollments
                set status='INACTIVE'
                where student_id=? and tenant_id=? and academic_year_id=?
                  and status='ACTIVE'
                  and coalesce(program_id,'00000000-0000-0000-0000-000000000000'::uuid)
                      = coalesce(?,'00000000-0000-0000-0000-000000000000'::uuid)
                """,
                id, tenantId, req.academicYearId(), effectiveProgramId
        );

        UUID enrollment = UUID.randomUUID();
        db.update("""
                insert into enrollments(
                    id,tenant_id,student_id,academic_year_id,class_id,stream_id,batch_id,program_id,status
                )
                values(?,?,?,?,?,?,?,?, 'ACTIVE')
                on conflict (tenant_id,student_id,academic_year_id,coalesce(program_id,'00000000-0000-0000-0000-000000000000'::uuid))
                do update set
                    class_id=excluded.class_id,
                    stream_id=excluded.stream_id,
                    batch_id=excluded.batch_id,
                    program_id=excluded.program_id,
                    status='ACTIVE',
                    enrolled_at=current_timestamp
                """,
                enrollment, tenantId, id, req.academicYearId(), req.classId(), req.streamId(), req.batchId(), effectiveProgramId
        );

        return db.queryForMap("""
                select e.id,e.tenant_id,e.student_id,e.academic_year_id,e.class_id,e.stream_id,e.batch_id,e.program_id,e.status,
                       b.name as batch_name,c.name as class_name,st.name as stream_name,ay.name as academic_year,
                       p.name as program_name
                from enrollments e
                join batches b on b.id=e.batch_id and b.tenant_id=e.tenant_id
                join classes c on c.id=e.class_id and c.tenant_id=e.tenant_id
                left join streams st on st.id=e.stream_id and st.tenant_id=e.tenant_id
                join academic_years ay on ay.id=e.academic_year_id and ay.tenant_id=e.tenant_id
                left join programs p on p.id=e.program_id and p.tenant_id=e.tenant_id
                where e.tenant_id=? and e.student_id=? and e.academic_year_id=?
                  and coalesce(e.program_id,'00000000-0000-0000-0000-000000000000'::uuid)
                      = coalesce(?,'00000000-0000-0000-0000-000000000000'::uuid)
                """, tenantId, id, req.academicYearId(), effectiveProgramId);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('settings.manage') or hasAuthority('students.delete')")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }

    private void requireTenantRow(String table, UUID id, UUID tenantId, String label) {
        if (id == null) {
            throw new ApiException("VALIDATION_ERROR", label + " is required");
        }
        Integer count = db.queryForObject(
                "select count(*) from " + table + " where id=? and tenant_id=?",
                Integer.class, id, tenantId
        );
        if (count == null || count == 0) {
            throw new ApiException("NOT_FOUND", label + " not found");
        }
    }

    private int count(String sql, Object... args) {
        Integer n = db.queryForObject(sql, Integer.class, args);
        return n == null ? 0 : n;
    }

    public record EnrollmentRequest(UUID academicYearId, UUID classId, UUID streamId, UUID batchId, UUID programId) {}
}
