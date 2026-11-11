package com.projexa.coaching.auth.controller;

import com.projexa.coaching.auth.service.RoleManagementService;
import com.projexa.coaching.common.tenant.TenantContextHolder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/roles")
@PreAuthorize("hasAuthority('roles.manage')")
public class RoleManagementController {
    private final RoleManagementService roles;
    public RoleManagementController(RoleManagementService roles) { this.roles = roles; }

    @GetMapping
    public List<Map<String,Object>> list() { return roles.list(TenantContextHolder.getRequired()); }

    @GetMapping("/permissions")
    public List<Map<String,Object>> permissions() { return roles.permissions(); }

    @GetMapping("/{roleId}")
    public Map<String,Object> get(@PathVariable UUID roleId) {
        return roles.get(TenantContextHolder.getRequired(), roleId);
    }

    @PostMapping
    public ResponseEntity<Map<String,UUID>> create(@Valid @RequestBody RoleRequest request) {
        UUID id = roles.create(TenantContextHolder.getRequired(), request.name(), request.description(), request.permissions());
        return ResponseEntity.status(201).body(Map.of("id", id));
    }

    @PutMapping("/{roleId}")
    public ResponseEntity<Void> update(@PathVariable UUID roleId, @Valid @RequestBody RoleRequest request) {
        roles.update(TenantContextHolder.getRequired(), roleId, request.name(), request.description(), request.permissions());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{roleId}")
    public ResponseEntity<Void> delete(@PathVariable UUID roleId) {
        roles.delete(TenantContextHolder.getRequired(), roleId);
        return ResponseEntity.noContent().build();
    }

    public record RoleRequest(@NotBlank @Size(max=80) String name,
                              @Size(max=300) String description,
                              List<@Size(max=150) String> permissions) {}
}
