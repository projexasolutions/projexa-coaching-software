package com.projexa.coaching.auth;

import com.projexa.coaching.auth.service.RoleManagementService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PhaseOneCustomRolesPostgresTest {
    @Test
    void customRolePrivilegeCeilingAndAssignmentsAreTenantScoped() {
        String url = System.getenv("PHASE1_TEST_DATABASE_URL");
        org.junit.jupiter.api.Assumptions.assumeTrue(url != null && !url.isBlank());
        String username = System.getenv().getOrDefault("PHASE1_TEST_DATABASE_USERNAME", "projexa");
        String password = System.getenv().getOrDefault("PHASE1_TEST_DATABASE_PASSWORD", "integration-test-only");
        Flyway.configure().dataSource(url, username, password).locations("classpath:db/migration").load().migrate();
        JdbcTemplate db = new JdbcTemplate(new DriverManagerDataSource(url, username, password));
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        UUID tenant = db.queryForObject("insert into tenants(name,slug,status) values(?,?,'ACTIVE') returning id",
            UUID.class, "Role Test", "roles-" + suffix);
        UUID foreignTenant = db.queryForObject("insert into tenants(name,slug,status) values(?,?,'ACTIVE') returning id",
            UUID.class, "Foreign Role Test", "foreign-roles-" + suffix);
        UUID manager = user(db, tenant, "manager-" + suffix + "@example.test");
        UUID colleague = user(db, tenant, "colleague-" + suffix + "@example.test");
        UUID foreignUser = user(db, foreignTenant, "foreign-" + suffix + "@example.test");

        UUID ownerRole = db.queryForObject("""
            insert into roles(tenant_id,name,code,system_role)
            values(?,'Role Manager',? ,true) returning id
            """, UUID.class, tenant, "ROLE_MANAGER_" + suffix);
        db.update("insert into user_roles(user_id,role_id) values(?,?)", manager, ownerRole);
        db.update("""
            insert into role_permissions(role_id,permission_id)
            select ?,id from permissions where code in ('roles.manage','students.read')
            """, ownerRole);
        RoleManagementService service = new RoleManagementService(db);

        UUID customRole = service.create(tenant, manager, "Student Viewer", "Read-only student access", List.of("students.read"));
        assertTrue(service.list(tenant).stream().anyMatch(row -> customRole.equals(row.get("id"))));
        assertThrows(IllegalArgumentException.class,
            () -> service.create(tenant, manager, "Finance Viewer", null, List.of("fees.read")));
        assertThrows(IllegalArgumentException.class,
            () -> service.create(tenant, manager, "Platform Admin", null, List.of("platform.institutes.manage")));
        assertThrows(IllegalArgumentException.class,
            () -> service.create(tenant, manager, "User Manager", null, List.of("users.manage")));

        service.assignUser(tenant, customRole, colleague);
        assertEquals(List.of(colleague), service.assignedUsers(tenant, customRole));
        assertThrows(IllegalArgumentException.class, () -> service.assignUser(tenant, customRole, foreignUser));
        assertThrows(IllegalArgumentException.class, () -> service.assignUser(foreignTenant, customRole, foreignUser));
        assertThrows(IllegalArgumentException.class, () -> service.get(foreignTenant, customRole));
        assertThrows(IllegalArgumentException.class, () -> service.delete(tenant, customRole));
        service.unassignUser(tenant, customRole, colleague);
        service.delete(tenant, customRole);
        assertFalse(service.list(tenant).stream().anyMatch(row -> customRole.equals(row.get("id"))));
    }

    private UUID user(JdbcTemplate db, UUID tenant, String email) {
        return db.queryForObject("""
            insert into users(tenant_id,email,password_hash,first_name,status)
            values(?,?,?,'Role User','ACTIVE') returning id
            """, UUID.class, tenant, email, "integration-test-password");
    }
}
