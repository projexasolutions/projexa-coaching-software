package com.projexa.coaching.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ResourceAccessTest {
  @Test void studentCanOnlyAccessOwnLinkedStudent() {
    JdbcTemplate db = mock(JdbcTemplate.class);
    ResourceAccess access = new ResourceAccess(db);
    UUID tenant = UUID.randomUUID(), student = UUID.randomUUID(), user = UUID.randomUUID();
    when(db.queryForObject(anyString(), eq(Integer.class), eq(student), eq(tenant), eq(user))).thenReturn(1);
    var auth = new UsernamePasswordAuthenticationToken(user, null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
    assertTrue(access.canAccessStudent(tenant, student, auth));
  }

  @Test void studentCannotAccessAnotherStudent() {
    JdbcTemplate db = mock(JdbcTemplate.class);
    ResourceAccess access = new ResourceAccess(db);
    UUID tenant = UUID.randomUUID(), student = UUID.randomUUID(), user = UUID.randomUUID();
    when(db.queryForObject(anyString(), eq(Integer.class), eq(student), eq(tenant), eq(user))).thenReturn(0);
    var auth = new UsernamePasswordAuthenticationToken(user, null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
    assertFalse(access.canAccessStudent(tenant, student, auth));
  }
}
