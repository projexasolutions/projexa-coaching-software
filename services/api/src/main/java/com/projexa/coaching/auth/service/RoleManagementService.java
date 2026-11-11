package com.projexa.coaching.auth.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class RoleManagementService {
    private static final Pattern CODE = Pattern.compile("[A-Z][A-Z0-9_]{2,39}");
    private static final Set<String> NEVER_ASSIGN = Set.of(
        "roles.manage", "users.manage", "settings.manage"
    );
    private final JdbcTemplate db;

    public RoleManagementService(JdbcTemplate db) { this.db = db; }

    @Transactional(readOnly = true)
    public List<Map<String,Object>> list(UUID tenantId) {
        return db.queryForList("""
            select r.id,r.name,r.code,r.description,r.system_role,
                   count(rp.permission_id) permission_count
            from roles r
            left join role_permissions rp on rp.role_id=r.id
            where r.tenant_id=?
            group by r.id,r.name,r.code,r.description,r.system_role
            order by r.system_role desc,r.name
            """, tenantId);
    }

    @Transactional(readOnly = true)
    public List<Map<String,Object>> permissions() {
        return db.queryForList("""
            select code,description from permissions
            where code not like 'platform.%'
              and code not in ('roles.manage','users.manage','settings.manage')
            order by code
            """);
    }

    @Transactional
    public UUID create(UUID tenantId, String rawName, String rawDescription, List<String> requested) {
        String name = clean(rawName, 80, "Role name");
        String description = rawDescription == null ? null : rawDescription.trim();
        if (description != null && description.length() > 300) throw new IllegalArgumentException("Description is too long.");
        List<String> codes = validatePermissions(requested);
        String code = "CUSTOM_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
        UUID roleId = db.queryForObject("""
            insert into roles(tenant_id,name,code,description,system_role)
            values(?,?,?,?,false) returning id
            """, UUID.class, tenantId, name, code, description);
        replacePermissions(roleId, codes);
        return roleId;
    }

    @Transactional
    public void update(UUID tenantId, UUID roleId, String rawName, String rawDescription, List<String> requested) {
        String name = clean(rawName, 80, "Role name");
        String description = rawDescription == null ? null : rawDescription.trim();
        if (description != null && description.length() > 300) throw new IllegalArgumentException("Description is too long.");
        List<String> codes = validatePermissions(requested);
        int changed = db.update("""
            update roles set name=?,description=?,updated_at=now()
            where id=? and tenant_id=? and system_role=false and code like 'CUSTOM_%'
            """, name, description, roleId, tenantId);
        if (changed != 1) throw new IllegalArgumentException("Custom role not found or cannot be edited.");
        replacePermissions(roleId, codes);
    }

    @Transactional
    public void delete(UUID tenantId, UUID roleId) {
        int changed = db.update("""
            delete from roles
            where id=? and tenant_id=? and system_role=false and code like 'CUSTOM_%'
              and not exists (select 1 from user_roles ur where ur.role_id=roles.id)
            """, roleId, tenantId);
        if (changed != 1) throw new IllegalArgumentException("Role is assigned to users, protected, or not found. Remove its assignments before deleting.");
    }

    private List<String> validatePermissions(List<String> requested) {
        if (requested == null) return List.of();
        if (requested.size() > 100) throw new IllegalArgumentException("Too many permissions.");
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String value : requested) {
            if (value == null || value.isBlank() || value.length() > 150) throw new IllegalArgumentException("Invalid permission.");
            String code = value.trim();
            if (code.startsWith("platform.") || NEVER_ASSIGN.contains(code)) throw new IllegalArgumentException("This permission cannot be assigned to a custom role.");
            unique.add(code);
        }
        if (unique.isEmpty()) return List.of();
        String placeholders = String.join(",", Collections.nCopies(unique.size(), "?"));
        List<Object> args = new ArrayList<>(unique);
        List<String> allowed = db.query("""
            select code from permissions
            where code not like 'platform.%'
              and code not in ('roles.manage','users.manage','settings.manage')
              and code in (""" + placeholders + ")",
            (rs,n) -> rs.getString(1), args.toArray());
        if (allowed.size() != unique.size()) throw new IllegalArgumentException("One or more permissions are unknown or restricted.");
        return List.copyOf(unique);
    }

    private void replacePermissions(UUID roleId, List<String> codes) {
        db.update("delete from role_permissions where role_id=?", roleId);
        if (codes.isEmpty()) return;
        String placeholders = String.join(",", Collections.nCopies(codes.size(), "?"));
        List<Object> args = new ArrayList<>();
        args.add(roleId);
        args.addAll(codes);
        db.update("""
            insert into role_permissions(role_id,permission_id)
            select ?,id from permissions where code in (""" + placeholders + ")
            on conflict do nothing", args.toArray());
    }

    private String clean(String value, int max, String label) {
        if (value == null || value.isBlank() || value.trim().length() > max)
            throw new IllegalArgumentException(label + " is required and must be at most " + max + " characters.");
        return value.trim();
    }
}
