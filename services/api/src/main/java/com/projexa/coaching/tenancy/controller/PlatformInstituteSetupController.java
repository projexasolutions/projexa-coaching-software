package com.projexa.coaching.tenancy.controller;

import com.projexa.coaching.common.exceptions.ApiException;
import com.projexa.coaching.common.responses.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/platform/institutes/{tenantId}/setup")
@PreAuthorize("hasAuthority('platform.institutes.read')")
public class PlatformInstituteSetupController {
    private static final Set<String> STEPS = Set.of(
        "INSTITUTE_PROFILE", "ACADEMIC_STRUCTURE", "PROGRAMS_EXAMS", "SUBJECTS",
        "BATCHES", "FACULTY", "FEE_PLANS", "DATA_IMPORT", "REVIEW_GO_LIVE"
    );
    private static final Set<String> STATUSES = Set.of("DRAFT", "CONFIGURING", "READY_FOR_REVIEW", "GO_LIVE");

    private final JdbcTemplate db;

    public PlatformInstituteSetupController(JdbcTemplate db) {
        this.db = db;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> overview(@PathVariable UUID tenantId, Authentication auth) {
        ensurePlatformAdmin(auth);
        ensureTenant(tenantId);
        ensureProfile(tenantId);

        Map<String, Object> profile = db.queryForMap("""
            select p.id,p.institute_type,p.setup_status,p.current_step,p.completed_steps,p.notes,
                   p.go_live_at,p.created_at,p.updated_at,
                   t.id tenant_id,t.name institute_name,t.slug,t.status tenant_status,
                   t.logo_url,t.primary_color,t.secondary_color,t.timezone,
                   t.contact_email,t.contact_phone,t.address
            from institute_setup_profiles p
            join tenants t on t.id=p.tenant_id
            where p.tenant_id=?
        """, tenantId);

        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("academicYears", count("academic_years", tenantId));
        counts.put("classes", count("classes", tenantId));
        counts.put("streams", count("streams", tenantId));
        counts.put("subjects", count("subjects", tenantId));
        counts.put("programs", count("programs", tenantId));
        counts.put("batches", count("batches", tenantId));
        counts.put("teachers", count("teachers", tenantId));
        counts.put("classrooms", count("classrooms", tenantId));
        counts.put("students", count("students", tenantId));

        return ApiResponse.ok(Map.of("profile", profile, "counts", counts));
    }

    @PutMapping("/profile")
    @PreAuthorize("hasAuthority('platform.institutes.manage')")
    public ApiResponse<Void> updateProfile(
            @PathVariable UUID tenantId,
            @RequestBody Map<String, Object> payload,
            Authentication auth) {
        ensurePlatformAdmin(auth);
        ensureTenant(tenantId);
        ensureProfile(tenantId);

        String name = text(payload.get("instituteName"));
        String slug = text(payload.get("slug"));
        if (name == null || name.length() > 150)
            throw new ApiException("VALIDATION_ERROR", "Institute name is required and must be 150 characters or fewer.");
        if (slug == null || !slug.matches("[a-z0-9-]{2,100}"))
            throw new ApiException("VALIDATION_ERROR", "Slug must contain 2-100 lowercase letters, numbers or hyphens.");

        Integer duplicate = db.queryForObject(
            "select count(*) from tenants where lower(slug)=lower(?) and id<>?",
            Integer.class, slug, tenantId);
        if (duplicate != null && duplicate > 0)
            throw new ApiException("VALIDATION_ERROR", "This institute slug is already in use.");

        db.update("""
            update tenants
            set name=?,slug=?,logo_url=?,primary_color=?,secondary_color=?,
                timezone=?,contact_email=?,contact_phone=?,address=?,updated_at=current_timestamp
            where id=?
        """, name, slug, nullable(payload.get("logoUrl")), nullable(payload.get("primaryColor")),
            nullable(payload.get("secondaryColor")), nullable(payload.get("timezone")),
            nullable(payload.get("contactEmail")), nullable(payload.get("contactPhone")),
            nullable(payload.get("address")), tenantId);

        db.update("""
            update institute_setup_profiles
            set institute_type=?,notes=?,updated_at=current_timestamp
            where tenant_id=?
        """, nullable(payload.get("instituteType")), nullable(payload.get("notes")), tenantId);

        audit(auth, "INSTITUTE_SETUP_PROFILE_UPDATED", tenantId,
            Map.of("name", name, "slug", slug));
        return ApiResponse.ok(null);
    }

    @PutMapping("/progress")
    @PreAuthorize("hasAuthority('platform.institutes.manage')")
    public ApiResponse<Void> progress(
            @PathVariable UUID tenantId,
            @RequestBody Map<String, Object> payload,
            Authentication auth) {
        ensureTenant(tenantId);
        ensureProfile(tenantId);

        String step = nullable(payload.get("currentStep"));
        String status = nullable(payload.get("status"));
        String completed = nullable(payload.get("completedSteps"));
        String notes = nullable(payload.get("notes"));

        if (step != null && !STEPS.contains(step))
            throw new ApiException("VALIDATION_ERROR", "Invalid setup step.");
        if (status != null && !STATUSES.contains(status))
            throw new ApiException("VALIDATION_ERROR", "Invalid setup status.");

        if (completed != null) {
            try {
                new com.fasterxml.jackson.databind.ObjectMapper().readTree(completed);
            } catch (Exception e) {
                throw new ApiException("VALIDATION_ERROR", "completedSteps must be valid JSON.");
            }
        }

        if ("GO_LIVE".equals(status)) {
            db.update("""
                update institute_setup_profiles
                set go_live_at=coalesce(go_live_at,current_timestamp)
                where tenant_id=?
            """, tenantId);
        }

        db.update("""
            update institute_setup_profiles
            set current_step=coalesce(?,current_step),
                setup_status=coalesce(?,setup_status),
                completed_steps=coalesce(?::jsonb,completed_steps),
                notes=coalesce(?,notes),
                updated_at=current_timestamp
            where tenant_id=?
        """, step, status, completed, notes, tenantId);

        audit(auth, "INSTITUTE_SETUP_PROGRESS_UPDATED", tenantId,
            Map.of("currentStep", String.valueOf(step), "status", String.valueOf(status)));
        return ApiResponse.ok(null);
    }

    private void ensurePlatformAdmin(Authentication auth) {
        UUID userId;
        try {
            userId = UUID.fromString(auth.getName());
        } catch (Exception e) {
            throw new ApiException("UNAUTHORIZED", "Platform administrator identity is invalid.");
        }
        Integer count = db.queryForObject(
            "select count(*) from platform_admins where user_id=? and status='ACTIVE'",
            Integer.class, userId);
        if (count == null || count == 0)
            throw new ApiException("FORBIDDEN", "Platform administrator access is required.");
    }

    private void ensureTenant(UUID tenantId) {
        Integer count = db.queryForObject(
            "select count(*) from tenants where id=?", Integer.class, tenantId);
        if (count == null || count == 0)
            throw new ApiException("NOT_FOUND", "Institute not found.");
    }

    private void ensureProfile(UUID tenantId) {
        db.update("""
            insert into institute_setup_profiles(tenant_id)
            values(?)
            on conflict (tenant_id) do nothing
        """, tenantId);
    }

    private int count(String table, UUID tenantId) {
        return Optional.ofNullable(db.queryForObject(
            "select count(*) from " + table + " where tenant_id=?",
            Integer.class, tenantId)).orElse(0);
    }

    private String text(Object value) {
        return nullable(value);
    }

    private String nullable(Object value) {
        if (value == null) return null;
        String valueText = String.valueOf(value).trim();
        return valueText.isBlank() ? null : valueText;
    }

    private void audit(Authentication auth, String action, UUID tenantId, Map<String, Object> metadata) {
        UUID adminId;
        try {
            adminId = UUID.fromString(auth.getName());
        } catch (Exception e) {
            throw new ApiException("UNAUTHORIZED", "Platform administrator identity is invalid.");
        }

        String json;
        try {
            json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(metadata);
        } catch (Exception e) {
            throw new ApiException("INTERNAL_ERROR", "Unable to write platform audit metadata.");
        }

        db.update(
            "insert into platform_admin_audit(admin_user_id,action,tenant_id,metadata) values(?,?,?,?::jsonb)",
            adminId, action, tenantId, json);
    }
}
