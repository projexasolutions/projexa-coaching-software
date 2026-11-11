package com.projexa.coaching.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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

    @Test
    void roleLookupIsTenantScopedAndDoesNotReturnAnotherInstitutesRole() {
        UUID tenant=UUID.randomUUID(), role=UUID.randomUUID();
        when(db.queryForMap(anyString(),eq(role),eq(tenant))).thenThrow(new EmptyResultDataAccessException(1));
        assertThrows(EmptyResultDataAccessException.class,()->service.get(tenant,role));
        verify(db).queryForMap(contains("where id=? and tenant_id=?"),eq(role),eq(tenant));
        verifyNoMoreInteractions(db);
    }

    @Test
    void deleteRequiresTenantScopeAndRefusesAssignedRoles() {
        UUID tenant=UUID.randomUUID(), role=UUID.randomUUID();
        when(db.update(anyString(),eq(role),eq(tenant))).thenReturn(0);
        assertThrows(IllegalArgumentException.class,()->service.delete(tenant,role));
        verify(db).update(contains("tenant_id=?"),eq(role),eq(tenant));
    }

    @Test
    void assigningCustomRoleRequiresRoleAndUserToBelongToSameTenant() {
        UUID tenant=UUID.randomUUID(), role=UUID.randomUUID(), user=UUID.randomUUID();
        when(db.queryForObject(contains("from roles"),eq(Integer.class),eq(role),eq(tenant))).thenReturn(1);
        when(db.queryForObject(contains("from users"),eq(Integer.class),eq(user),eq(tenant))).thenReturn(1);
        service.assignUser(tenant,role,user);
        verify(db).update(contains("insert into user_roles"),eq(user),eq(role));
    }

    @Test
    void customRoleAssignmentRejectsRoleFromAnotherTenant() {
        UUID tenant=UUID.randomUUID(), role=UUID.randomUUID(), user=UUID.randomUUID();
        when(db.queryForObject(contains("from roles"),eq(Integer.class),eq(role),eq(tenant))).thenReturn(0);
        assertThrows(IllegalArgumentException.class,()->service.assignUser(tenant,role,user));
        verify(db,never()).update(anyString(),any(),any());
    }
}
