package com.projexa.coaching.auth.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class RoleManagementService {
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
    public Map<String,Object> get(UUID tenantId, UUID roleId) {
        Map<String,Object> role = db.queryForMap("""
            select id,name,code,description,system_role
            from roles where id=? and tenant_id=? and system_role=false and left(code,7)='CUSTOM_'
            """, roleId, tenantId);
        role.put("permissions", db.query("""
            select p.code from role_permissions rp
            join permissions p on p.id=rp.permission_id
            where rp.role_id=? order by p.code
            """, (rs,n) -> rs.getString(1), roleId));
        return role;
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
    public UUID create(UUID tenantId, UUID actorId, String rawName, String rawDescription, List<String> requested) {
        String name = clean(rawName, 80, "Role name");
        String description = rawDescription == null ? null : rawDescription.trim();
        if (description != null && description.length() > 300) throw new IllegalArgumentException("Description is too long.");
        List<String> codes = validatePermissions(actorId, requested);
        String code = "CUSTOM_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
        UUID roleId = db.queryForObject("""
            insert into roles(tenant_id,name,code,description,system_role)
            values(?,?,?,?,false) returning id
            """, UUID.class, tenantId, name, code, description);
        replacePermissions(roleId, codes);
        return roleId;
    }

    @Transactional
    public void update(UUID tenantId, UUID actorId, UUID roleId, String rawName, String rawDescription, List<String> requested) {
        String name = clean(rawName, 80, "Role name");
        String description = rawDescription == null ? null : rawDescription.trim();
        if (description != null && description.length() > 300) throw new IllegalArgumentException("Description is too long.");
        List<String> codes = validatePermissions(actorId, requested);
        int changed = db.update("""
            update roles set name=?,description=?,updated_at=now()
            where id=? and tenant_id=? and system_role=false and left(code,7)='CUSTOM_'
            """, name, description, roleId, tenantId);
        if (changed != 1) throw new IllegalArgumentException("Custom role not found or cannot be edited.");
        replacePermissions(roleId, codes);
    }

    @Transactional
    public void delete(UUID tenantId, UUID roleId) {
        int changed = db.update("""
            delete from roles
            where id=? and tenant_id=? and system_role=false and left(code,7)='CUSTOM_' and not exists (select 1 from user_roles ur where ur.role_id=roles.id)
            """, roleId, tenantId);
        if (changed != 1) throw new IllegalArgumentException("Role is assigned to users, protected, or not found. Remove its assignments before deleting.");
    }


    @Transactional(readOnly = true)
    public List<Map<String,Object>> users(UUID tenantId) {
        return db.queryForList("""
            select id,email,first_name,last_name,status
            from users where tenant_id=? and status='ACTIVE'
            order by first_name,last_name,email
            """, tenantId);
    }

    @Transactional(readOnly = true)
    public List<UUID> assignedUsers(UUID tenantId, UUID roleId) {
        requireCustomRole(tenantId, roleId);
        return db.query("""
            select ur.user_id from user_roles ur
            join users u on u.id=ur.user_id and u.tenant_id=?
            join roles r on r.id=ur.role_id and r.tenant_id=?
            where r.id=? order by ur.user_id
            """, (rs,n) -> rs.getObject(1, UUID.class), tenantId, tenantId, roleId);
    }

    @Transactional
    public void assignUser(UUID tenantId, UUID roleId, UUID userId) {
        requireCustomRole(tenantId, roleId);
        Integer userExists = db.queryForObject("""
            select count(*) from users where id=? and tenant_id=? and status='ACTIVE'
            """, Integer.class, userId, tenantId);
        if (userExists == null || userExists != 1)
            throw new IllegalArgumentException("Active user was not found in this institute.");
        db.update("insert into user_roles(user_id,role_id) values(?,?) on conflict do nothing", userId, roleId);
    }

    @Transactional
    public void unassignUser(UUID tenantId, UUID roleId, UUID userId) {
        requireCustomRole(tenantId, roleId);
        db.update("""
            delete from user_roles ur using roles r
            where ur.role_id=r.id and ur.user_id=? and r.id=? and r.tenant_id=?
              and r.system_role=false and left(r.code,7)='CUSTOM_'
            """, userId, roleId, tenantId);
    }

    private void requireCustomRole(UUID tenantId, UUID roleId) {
        Integer exists = db.queryForObject("""
            select count(*) from roles
            where id=? and tenant_id=? and system_role=false and left(code,7)='CUSTOM_'
            """, Integer.class, roleId, tenantId);
        if (exists == null || exists != 1)
            throw new IllegalArgumentException("Custom role not found or cannot be assigned.");
    }

    private List<String> validatePermissions(UUID actorId, List<String> requested) {
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
        String sql = "select distinct p.code from permissions p " +
            "join role_permissions rp on rp.permission_id=p.id " +
            "join user_roles ur on ur.role_id=rp.role_id " +
            "where ur.user_id=? and p.code not like 'platform.%' " +
            "and p.code not in ('roles.manage','users.manage','settings.manage') and p.code in (" + placeholders + ")";
        args.add(0, actorId);
        List<String> allowed = db.query(sql, (rs,n) -> rs.getString(1), args.toArray());
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
        String sql = "insert into role_permissions(role_id,permission_id) " +
            "select ?,id from permissions where code in (" + placeholders + ") on conflict do nothing";
        db.update(sql, args.toArray());
    }

    private String clean(String value, int max, String label) {
        if (value == null || value.isBlank() || value.trim().length() > max)
            throw new IllegalArgumentException(label + " is required and must be at most " + max + " characters.");
        return value.trim();
    }
}
