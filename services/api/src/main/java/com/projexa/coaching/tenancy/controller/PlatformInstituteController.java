package com.projexa.coaching.tenancy.controller;

import com.projexa.coaching.common.exceptions.ApiException;
import com.projexa.coaching.common.responses.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/platform/institutes")
@PreAuthorize("hasAuthority('platform.institutes.read')")
public class PlatformInstituteController {
    private final JdbcTemplate db;

    public PlatformInstituteController(JdbcTemplate db) {
        this.db = db;
    }

    @GetMapping
    public ApiResponse<List<Map<String,Object>>> list(Authentication auth) {
        ensurePlatformAdmin(auth);
        return ApiResponse.ok(db.queryForList("""
            select t.id,t.name,t.slug,t.status,t.contact_email,t.contact_phone,t.timezone,
                   p.setup_status,p.current_step,p.go_live_at,
                   coalesce((select count(*) from students s where s.tenant_id=t.id),0) students,
                   coalesce((select count(*) from programs pr where pr.tenant_id=t.id),0) programs,
                   coalesce((select count(*) from batches b where b.tenant_id=t.id),0) batches
            from tenants t
            left join institute_setup_profiles p on p.tenant_id=t.id
            order by t.created_at desc
        """));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String,Object>> get(@PathVariable UUID id, Authentication auth) {
        ensurePlatformAdmin(auth);
        return ApiResponse.ok(detail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('platform.institutes.manage')")
    public ApiResponse<Map<String,Object>> create(
            @RequestBody Map<String,Object> p,
            Authentication auth) {
        ensurePlatformAdmin(auth);

        String name = text(p.get("name"));
        String slug = text(p.get("slug"));
        if (name == null || name.length() > 150)
            throw new ApiException("VALIDATION_ERROR", "Institute name is required and must be 150 characters or fewer.");
        if (slug == null || !slug.matches("[a-z0-9-]{2,100}"))
            throw new ApiException("VALIDATION_ERROR", "Slug must contain 2-100 lowercase letters, numbers or hyphens.");

        Integer exists = db.queryForObject(
            "select count(*) from tenants where lower(slug)=lower(?)",
            Integer.class, slug);
        if (exists != null && exists > 0)
            throw new ApiException("VALIDATION_ERROR", "Institute slug already exists.");

        UUID tenant = UUID.randomUUID();
        db.update("""
            insert into tenants(id,name,slug,status,timezone,contact_email,contact_phone,address)
            values(?,?,?,?,?,?,?,?)
        """, tenant, name, slug, "ACTIVE",
            nullable(p.get("timezone"), "Asia/Kolkata"),
            nullable(p.get("contactEmail"), null),
            nullable(p.get("contactPhone"), null),
            nullable(p.get("address"), null));

        db.update(
            "insert into institute_setup_profiles(tenant_id,institute_type) values(?,?)",
            tenant, nullable(p.get("instituteType"), "OTHER"));

        audit(auth, "INSTITUTE_CREATED", tenant, Map.of("name", name, "slug", slug));
        return ApiResponse.ok(Map.of("id", tenant));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('platform.institutes.manage')")
    public ApiResponse<Void> status(
            @PathVariable UUID id,
            @RequestBody Map<String,Object> p,
            Authentication auth) {
        ensurePlatformAdmin(auth);

        ensureTenant(id);
        String status = text(p.get("status"));
        if (!Set.of("ACTIVE", "SUSPENDED", "ARCHIVED").contains(status))
            throw new ApiException("VALIDATION_ERROR", "Invalid institute status.");

        db.update(
            "update tenants set status=?,updated_at=current_timestamp where id=?",
            status, id);
        audit(auth, "INSTITUTE_STATUS_CHANGED", id, Map.of("status", status));
        return ApiResponse.ok(null);
    }

    private Map<String,Object> detail(UUID id) {
        try {
            return db.queryForMap("""
                select t.*,p.setup_status,p.current_step,p.completed_steps,p.notes,p.go_live_at
                from tenants t
                left join institute_setup_profiles p on p.tenant_id=t.id
                where t.id=?
            """, id);
        } catch (Exception e) {
            throw new ApiException("NOT_FOUND", "Institute not found.");
        }
    }

    private void ensureTenant(UUID id) {
        Integer count = db.queryForObject(
            "select count(*) from tenants where id=?", Integer.class, id);
        if (count == null || count == 0)
            throw new ApiException("NOT_FOUND", "Institute not found.");
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

    private void audit(Authentication auth, String action, UUID tenant, Map<String,Object> metadata) {
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
            adminId, action, tenant, json);
    }

    private String text(Object value) {
        return nullable(value, null);
    }

    private String nullable(Object value, String fallback) {
        if (value == null) return fallback;
        String valueText = String.valueOf(value).trim();
        return valueText.isBlank() ? fallback : valueText;
    }
}
