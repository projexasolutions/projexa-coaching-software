package com.projexa.coaching.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoleManagementServiceTest {
    private final JdbcTemplate db = mock(JdbcTemplate.class);
    private final RoleManagementService service = new RoleManagementService(db);

    @Test
    void customRoleCannotReceivePlatformPermissions() {
        assertThrows(IllegalArgumentException.class, () ->
            service.create(UUID.randomUUID(), UUID.randomUUID(), "Auditor", "Read-only role", List.of("platform.institutes.manage")));
        verifyNoInteractions(db);
    }

    @Test
    void customRoleCannotReceiveRoleOrUserAdministrationPermissions() {
        assertThrows(IllegalArgumentException.class, () ->
            service.create(UUID.randomUUID(), UUID.randomUUID(), "Coordinator", null, List.of("users.manage")));
        assertThrows(IllegalArgumentException.class, () ->
            service.create(UUID.randomUUID(), UUID.randomUUID(), "Coordinator", null, List.of("roles.manage")));
        verifyNoInteractions(db);
    }

    @Test
    void customRoleRejectsMoreThanOneHundredPermissions() {
        List<String> codes = java.util.stream.IntStream.range(0,101).mapToObj(i -> "permission."+i).toList();
        assertThrows(IllegalArgumentException.class, () ->
            service.create(UUID.randomUUID(), UUID.randomUUID(), "Coordinator", null, codes));
        verifyNoInteractions(db);
    }

    @Test
    void invalidRoleNameIsRejectedBeforeDatabaseAccess() {
        assertThrows(IllegalArgumentException.class, () ->
            service.create(UUID.randomUUID(), UUID.randomUUID(), "  ", null, List.of()));
        verifyNoInteractions(db);
    }
}
