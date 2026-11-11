package com.projexa.coaching.auth;

import com.projexa.coaching.common.security.ResourceAccess;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PhaseOnePortalScopePostgresTest {
    @Test
    void studentParentAndTeacherCanOnlyAccessTheirAuthorizedStudentRecords() {
        String url = System.getenv("PHASE1_TEST_DATABASE_URL");
        org.junit.jupiter.api.Assumptions.assumeTrue(url != null && !url.isBlank());
        String username = System.getenv().getOrDefault("PHASE1_TEST_DATABASE_USERNAME", "projexa");
        String password = System.getenv().getOrDefault("PHASE1_TEST_DATABASE_PASSWORD", "integration-test-only");
        Flyway.configure().dataSource(url, username, password).locations("classpath:db/migration").load().migrate();
        JdbcTemplate db = new JdbcTemplate(new DriverManagerDataSource(url, username, password));
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        UUID tenant = db.queryForObject("insert into tenants(name,slug,status) values(?,?,'ACTIVE') returning id",
            UUID.class, "Portal Scope Test", "scope-" + suffix);
        UUID otherTenant = db.queryForObject("insert into tenants(name,slug,status) values(?,?,'ACTIVE') returning id",
            UUID.class, "Other Portal Institute", "other-scope-" + suffix);

        UUID studentUser = user(db, tenant, "student-" + suffix + "@example.test");
        UUID parentUser = user(db, tenant, "parent-" + suffix + "@example.test");
        UUID teacherUser = user(db, tenant, "teacher-" + suffix + "@example.test");

        UUID studentA = db.queryForObject("""
            insert into students(tenant_id,admission_number,first_name,user_id)
            values(?,?,'Student A',?) returning id
            """, UUID.class, tenant, "A-" + suffix, studentUser);
        UUID studentB = db.queryForObject("""
            insert into students(tenant_id,admission_number,first_name)
            values(?,?,'Student B') returning id
            """, UUID.class, tenant, "B-" + suffix);
        UUID foreignStudent = db.queryForObject("""
            insert into students(tenant_id,admission_number,first_name)
            values(?,?,'Foreign Student') returning id
            """, UUID.class, otherTenant, "F-" + suffix);

        UUID parent = db.queryForObject("""
            insert into parents(tenant_id,first_name,email,user_id)
            values(?,'Parent',?,?) returning id
            """, UUID.class, tenant, "parent-" + suffix + "@example.test", parentUser);
        db.update("insert into student_parents(student_id,parent_id,relation,is_primary) values(?,?,'Parent',true)", studentA, parent);

        UUID teacher = db.queryForObject("""
            insert into teachers(tenant_id,user_id,first_name,email)
            values(?,?,'Teacher',?) returning id
            """, UUID.class, tenant, teacherUser, "teacher-" + suffix + "@example.test");

        UUID year = db.queryForObject("""
            insert into academic_years(tenant_id,name,start_date,end_date,is_current)
            values(?,'2026-27',date '2026-04-01',date '2027-03-31',true) returning id
            """, UUID.class, tenant);
        UUID classroom = db.queryForObject("insert into classrooms(tenant_id,name) values(?,'Scope Room') returning id", UUID.class, tenant);
        UUID classId = db.queryForObject("insert into classes(tenant_id,name) values(?,'Scope Class') returning id", UUID.class, tenant);
        UUID batchA = db.queryForObject("""
            insert into batches(tenant_id,academic_year_id,class_id,name)
            values(?,?,?,'Assigned Batch') returning id
            """, UUID.class, tenant, year, classId);
        UUID batchB = db.queryForObject("""
            insert into batches(tenant_id,academic_year_id,class_id,name)
            values(?,?,?,'Unassigned Batch') returning id
            """, UUID.class, tenant, year, classId);
        db.update("insert into enrollments(tenant_id,student_id,academic_year_id,class_id,batch_id) values(?,?,?,?,?)",
            tenant, studentA, year, classId, batchA);
        db.update("insert into enrollments(tenant_id,student_id,academic_year_id,class_id,batch_id) values(?,?,?,?,?)",
            tenant, studentB, year, classId, batchB);
        db.update("insert into teacher_batch_assignments(teacher_id,batch_id) values(?,?)", teacher, batchA);

        ResourceAccess access = new ResourceAccess(db);
        var studentAuth = auth(studentUser, "ROLE_STUDENT");
        var parentAuth = auth(parentUser, "ROLE_PARENT");
        var teacherAuth = auth(teacherUser, "ROLE_TEACHER");

        assertTrue(access.canAccessStudent(tenant, studentA, studentAuth));
        assertFalse(access.canAccessStudent(tenant, studentB, studentAuth));
        assertFalse(access.canAccessStudent(tenant, foreignStudent, studentAuth));

        assertTrue(access.canAccessStudent(tenant, studentA, parentAuth));
        assertFalse(access.canAccessStudent(tenant, studentB, parentAuth));
        assertFalse(access.canAccessStudent(tenant, foreignStudent, parentAuth));

        assertTrue(access.canAccessStudent(tenant, studentA, teacherAuth));
        assertFalse(access.canAccessStudent(tenant, studentB, teacherAuth));
        assertFalse(access.canAccessStudent(tenant, foreignStudent, teacherAuth));
    }

    private UUID user(JdbcTemplate db, UUID tenant, String email) {
        return db.queryForObject("""
            insert into users(tenant_id,email,password_hash,first_name,status)
            values(?,?,?,'Portal User','ACTIVE') returning id
            """, UUID.class, tenant, email, "integration-test-password");
    }

    private UsernamePasswordAuthenticationToken auth(UUID userId, String role) {
        return new UsernamePasswordAuthenticationToken(userId, null,
            List.of(new SimpleGrantedAuthority(role)));
    }
}
