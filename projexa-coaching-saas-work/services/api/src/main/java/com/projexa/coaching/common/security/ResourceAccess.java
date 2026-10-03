package com.projexa.coaching.common.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ResourceAccess {
  private final JdbcTemplate db;
  public ResourceAccess(JdbcTemplate db) { this.db = db; }

  public UUID userId(Authentication auth) { return UUID.fromString(auth.getName()); }

  public boolean isStaff(Authentication auth) {
    return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_INSTITUTE_OWNER") || a.getAuthority().equals("ROLE_INSTITUTE_ADMIN"));
  }

  public boolean canAccessStudent(UUID tenantId, UUID studentId, Authentication auth) {
    if (isStaff(auth)) return existsStudent(tenantId, studentId);
    UUID userId = userId(auth);
    if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"))) {
      return db.queryForObject("select count(*) from students where id=? and tenant_id=? and user_id=?", Integer.class, studentId, tenantId, userId) > 0;
    }
    if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PARENT"))) {
      return db.queryForObject("select count(*) from student_parents sp join parents p on p.id=sp.parent_id where sp.student_id=? and p.tenant_id=? and p.user_id=?", Integer.class, studentId, tenantId, userId) > 0;
    }
    if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"))) {
      return db.queryForObject("select count(*) from enrollments e join teacher_batch_assignments tba on tba.batch_id=e.batch_id join teachers t on t.id=tba.teacher_id where e.student_id=? and e.tenant_id=? and t.user_id=? and e.status='ACTIVE'", Integer.class, studentId, tenantId, userId) > 0;
    }
    return false;
  }

  public UUID requireOwnStudent(UUID tenantId, UUID requestedStudentId, Authentication auth) {
    if (!canAccessStudent(tenantId, requestedStudentId, auth)) throw new org.springframework.security.access.AccessDeniedException("Student resource is not accessible to this user");
    return requestedStudentId;
  }

  public boolean existsStudent(UUID tenantId, UUID studentId) {
    Integer count = db.queryForObject("select count(*) from students where id=? and tenant_id=?", Integer.class, studentId, tenantId);
    return count != null && count > 0;
  }
}
