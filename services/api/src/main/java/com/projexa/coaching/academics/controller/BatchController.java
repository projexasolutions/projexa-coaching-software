package com.projexa.coaching.academics.controller;

import com.projexa.coaching.academics.entity.Batch;
import com.projexa.coaching.academics.service.BatchService;
import com.projexa.coaching.common.responses.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/batches")
public class BatchController {
    private final BatchService service;

    public BatchController(BatchService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('dashboard.read') or hasAuthority('settings.manage') or hasAuthority('students.read')")
    public ApiResponse<List<Batch>> list() {
        return ApiResponse.ok(service.list());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('dashboard.read') or hasAuthority('settings.manage') or hasAuthority('students.read')")
    public ApiResponse<Batch> get(@PathVariable UUID id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('settings.manage') or hasAuthority('students.create')")
    public ApiResponse<Batch> create(@RequestBody Batch item) {
        return ApiResponse.ok(service.create(item));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('settings.manage') or hasAuthority('students.update')")
    public ApiResponse<Batch> update(@PathVariable UUID id, @RequestBody Batch item) {
        return ApiResponse.ok(service.update(id, item));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('settings.manage') or hasAuthority('students.delete')")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }
}
