package com.projexa.coaching.tenancy.controller;

import com.projexa.coaching.common.responses.ApiResponse;
import com.projexa.coaching.common.exceptions.ApiException;
import com.projexa.coaching.common.tenant.TenantContextHolder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/institute-setup")
public class InstituteSetupController {
    private final JdbcTemplate db;
    private final ObjectMapper mapper;

    public InstituteSetupController(JdbcTemplate db, ObjectMapper mapper) {
        this.db = db;
        this.mapper = mapper;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('setup.read') or hasAuthority('settings.manage') or hasAuthority('dashboard.read')")
    public ApiResponse<Map<String,Object>> overview() {
        UUID t = TenantContextHolder.getRequired();
        ensureProfile(t);

        Map<String,Object> profile = db.queryForMap("""
            select p.id,p.institute_type,p.setup_status,p.current_step,p.completed_steps,p.notes,
                   p.go_live_at,p.created_at,p.updated_at,
                   tn.id tenant_id,tn.name institute_name,tn.slug,tn.status tenant_status,
                   tn.logo_url,tn.primary_color,tn.secondary_color,tn.timezone,
                   tn.contact_email,tn.contact_phone,tn.address
            from institute_setup_profiles p
            join tenants tn on tn.id=p.tenant_id
            where p.tenant_id=?
        """, t);

        Map<String,Object> counts = new LinkedHashMap<>();
        counts.put("academicYears", count("academic_years", t));
        counts.put("classes", count("classes", t));
        counts.put("streams", count("streams", t));
        counts.put("subjects", count("subjects", t));
        counts.put("programs", count("programs", t));
        counts.put("batches", count("batches", t));
        counts.put("teachers", count("teachers", t));
        counts.put("classrooms", count("classrooms", t));
        counts.put("students", count("students", t));

        Map<String,Object> readiness = readiness(t, profile, counts);
        return ApiResponse.ok(Map.of("profile", profile, "counts", counts, "readiness", readiness));
    }

    @PutMapping("/profile")
    @PreAuthorize("hasAuthority('setup.manage') or hasAuthority('settings.manage')")
    public ApiResponse<Void> updateProfile(@RequestBody Map<String,Object> p) {
        UUID t = TenantContextHolder.getRequired();
        ensureProfile(t);

        String name = text(p.get("instituteName"));
        String slug = text(p.get("slug"));
        if (name == null || name.length() > 150) throw new ApiException("VALIDATION_ERROR", "Institute name is required and must be 150 characters or fewer.");
        if (slug == null || !slug.matches("[a-z0-9-]{2,100}")) throw new ApiException("VALIDATION_ERROR", "Slug must contain 2-100 lowercase letters, numbers or hyphens.");

        Integer duplicate = db.queryForObject("select count(*) from tenants where lower(slug)=lower(?) and id<>?", Integer.class, slug, t);
        if (duplicate != null && duplicate > 0) throw new ApiException("DUPLICATE_SLUG", "This institute slug is already in use.");

        db.update("""
            update tenants set name=?,slug=?,logo_url=?,primary_color=?,secondary_color=?,
                   timezone=?,contact_email=?,contact_phone=?,address=?,updated_at=current_timestamp
            where id=?
        """, name, slug, nullable(p.get("logoUrl")), nullable(p.get("primaryColor")),
                nullable(p.get("secondaryColor")), Optional.ofNullable(nullable(p.get("timezone"))).orElse("Asia/Kolkata"),
                nullable(p.get("contactEmail")), nullable(p.get("contactPhone")),
                nullable(p.get("address")), t);

        db.update("""
            update institute_setup_profiles set institute_type=?,notes=?,updated_at=current_timestamp
            where tenant_id=?
        """, nullable(p.get("instituteType")), nullable(p.get("notes")), t);
        return ApiResponse.ok(null);
    }

    @PutMapping("/progress")
    @PreAuthorize("hasAuthority('setup.manage') or hasAuthority('settings.manage')")
    public ApiResponse<Void> progress(@RequestBody Map<String,Object> p) {
        UUID t = TenantContextHolder.getRequired();
        ensureProfile(t);

        String step = nullable(p.get("currentStep"));
        String status = nullable(p.get("status"));
        String completed = nullable(p.get("completedSteps"));
        String notes = nullable(p.get("notes"));

        if (completed != null) {
            try {
                JsonNode node = mapper.readTree(completed);
                if (!node.isArray()) throw new IllegalArgumentException();
            } catch (Exception e) {
                throw new ApiException("VALIDATION_ERROR", "completedSteps must be a JSON array");
            }
        }

        if (step != null && !Set.of(
                "INSTITUTE_PROFILE","ACADEMIC_STRUCTURE","PROGRAMS_EXAMS","SUBJECTS",
                "BATCHES","FACULTY","FEE_PLANS","DATA_IMPORT","REVIEW_GO_LIVE"
        ).contains(step)) throw new IllegalArgumentException("Invalid setup step.");

        if (status != null && !Set.of("DRAFT","CONFIGURING","READY_FOR_REVIEW","GO_LIVE").contains(status))
            throw new ApiException("VALIDATION_ERROR", "Invalid setup status.");

        if (status != null && status.equals("GO_LIVE")) {
            Map<String,Object> profile = db.queryForMap("select tn.name institute_name,tn.slug,p.institute_type from institute_setup_profiles p join tenants tn on tn.id=p.tenant_id where p.tenant_id=?", t);
            Map<String,Object> counts = new LinkedHashMap<>();
            counts.put("academicYears", count("academic_years", t));
            counts.put("classes", count("classes", t));
            counts.put("subjects", count("subjects", t));
            counts.put("batches", count("batches", t));
            counts.put("teachers", count("teachers", t));
            Map<String,Object> readiness = readiness(t, profile, counts);
            if (!Boolean.TRUE.equals(readiness.get("ready"))) {
                throw new ApiException("SETUP_NOT_READY", "Institute setup is not ready for go-live. Complete the required setup items first.");
            }
            db.update("update institute_setup_profiles set go_live_at=current_timestamp where tenant_id=?", t);
        }

        db.update("""
            update institute_setup_profiles
            set current_step=coalesce(?,current_step),
                setup_status=coalesce(?,setup_status),
                completed_steps=coalesce(?::jsonb,completed_steps),
                notes=coalesce(?,notes),
                updated_at=current_timestamp
            where tenant_id=?
        """, step, status, completed, notes, t);

        return ApiResponse.ok(null);
    }

    private void ensureProfile(UUID t) {
        db.update("""
            insert into institute_setup_profiles(tenant_id)
            values(?)
            on conflict (tenant_id) do nothing
        """, t);
    }

    private Map<String,Object> readiness(UUID tenant, Map<String,Object> profile, Map<String,Object> counts) {
        List<Map<String,Object>> checks = new ArrayList<>();
        checks.add(check("Institute profile", profile.get("institute_name") != null && profile.get("slug") != null && profile.get("institute_type") != null, "Name, slug and institute type are configured."));
        checks.add(check("Academic year", number(counts.get("academicYears")) > 0, "At least one academic year is required."));
        checks.add(check("Classes", number(counts.get("classes")) > 0, "At least one class is required."));
        checks.add(check("Subjects", number(counts.get("subjects")) > 0, "At least one subject is required."));
        checks.add(check("Batches", number(counts.get("batches")) > 0, "At least one batch is required."));
        checks.add(check("Faculty", number(counts.get("teachers")) > 0, "At least one faculty member is required."));
        boolean ready = checks.stream().allMatch(x -> Boolean.TRUE.equals(x.get("ready")));
        return Map.of("ready", ready, "checks", checks);
    }

    private Map<String,Object> check(String name, boolean ready, String detail) {
        return Map.of("name", name, "ready", ready, "detail", detail);
    }

    private int number(Object value) {
        return value instanceof Number n ? n.intValue() : 0;
    }

    private int count(String table, UUID tenant) {
        return Optional.ofNullable(db.queryForObject(
                "select count(*) from " + table + " where tenant_id=?", Integer.class, tenant)).orElse(0);
    }

    private String text(Object value) {
        return nullable(value);
    }

    private String nullable(Object value) {
        if (value == null) return null;
        String s = String.valueOf(value).trim();
        return s.isBlank() ? null : s;
    }
}
