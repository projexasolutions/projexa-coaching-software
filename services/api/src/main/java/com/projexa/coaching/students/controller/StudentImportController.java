package com.projexa.coaching.students.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projexa.coaching.common.exceptions.ApiException;
import com.projexa.coaching.common.tenant.TenantContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/v1/students/import")
public class StudentImportController {
    private static final Set<String> REQUIRED = Set.of("admissionNumber", "firstName");
    private final JdbcTemplate db;
    private final ObjectMapper mapper;

    public StudentImportController(JdbcTemplate db, ObjectMapper mapper) {
        this.db = db;
        this.mapper = mapper;
    }

    @PostMapping("/preview")
    @PreAuthorize("hasAuthority('students.import') or hasAuthority('settings.manage')")
    public Map<String,Object> preview(@RequestParam("file") MultipartFile file,
                                      @RequestParam(value="mapping", required=false) String mappingJson) {
        return process(file, mappingJson, false);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('students.create') or hasAuthority('settings.manage')")
    public Map<String,Object> importStudents(@RequestParam("file") MultipartFile file,
                                             @RequestParam(value="mapping", required=false) String mappingJson) {
        return process(file, mappingJson, true);
    }

    private Map<String,Object> process(MultipartFile file, String mappingJson, boolean commit) {
        if (file == null || file.isEmpty()) throw new ApiException("VALIDATION_ERROR", "CSV file is required");
        if (file.getSize() > 5 * 1024 * 1024) throw new ApiException("FILE_TOO_LARGE", "Student import CSV must be 5 MB or smaller");
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("students.csv");
        if (!name.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new ApiException("UNSUPPORTED_FILE", "Student import currently supports CSV files.");
        }

        UUID tenant = TenantContextHolder.getRequired();
        Map<String,String> mapping = parseMapping(mappingJson);
        List<List<String>> rows = parseCsv(new String(read(file), StandardCharsets.UTF_8));
        if (rows.isEmpty()) throw new ApiException("VALIDATION_ERROR", "CSV is empty");

        List<String> headers = rows.get(0).stream().map(this::normalizeHeader).toList();
        if (headers.stream().anyMatch(String::isBlank)) throw new ApiException("VALIDATION_ERROR", "CSV contains an empty header");
        if (new HashSet<>(headers).size() != headers.size()) throw new ApiException("VALIDATION_ERROR", "CSV contains duplicate column headers");
        if (!headers.contains(normalizeHeader("admissionNumber")) || !headers.contains(normalizeHeader("firstName"))) {
            throw new ApiException("VALIDATION_ERROR", "CSV must contain admissionNumber and firstName columns");
        }
        List<Map<String,Object>> errors = new ArrayList<>();
        List<Map<String,String>> valid = new ArrayList<>();
        Set<String> seenAdmissions = new HashSet<>();

        for (int i = 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            if (row.stream().allMatch(x -> x == null || x.trim().isEmpty())) continue;
            Map<String,String> data = new LinkedHashMap<>();
            for (int c = 0; c < headers.size(); c++) {
                data.put(headers.get(c), c < row.size() ? row.get(c).trim() : "");
            }
            Map<String,String> canonical = canonicalize(data, mapping);
            List<String> rowErrors = validate(canonical, tenant);
            String admission = nullable(canonical.get("admissionNumber"));
            if (admission != null && !seenAdmissions.add(admission)) rowErrors.add("Duplicate admission number in this CSV");
            if (rowErrors.isEmpty()) valid.add(canonical);
            else errors.add(Map.of("row", i + 1, "errors", rowErrors));
        }

        if (commit && !errors.isEmpty()) {
            throw new ApiException("IMPORT_VALIDATION_FAILED", "Resolve all import validation errors before committing the file");
        }

        int imported = 0;
        if (commit) {
            for (Map<String,String> s : valid) {
                if (insertStudent(tenant, s)) imported++;
            }
        }

        if (commit) {
            db.update("""
                insert into student_import_runs(id,tenant_id,file_name,file_type,mode,total_rows,valid_rows,imported_rows,failed_rows,status)
                values(?,?,?,?,?,?,?,?,?,?)
                """, UUID.randomUUID(), tenant, name, "CSV", "IMPORT", valid.size() + errors.size(),
                valid.size(), imported, errors.size(), errors.isEmpty() ? "COMPLETED" : "COMPLETED_WITH_ERRORS");
        }

        return Map.of(
                "fileName", name,
                "totalRows", valid.size() + errors.size(),
                "validRows", valid.size(),
                "failedRows", errors.size(),
                "importedRows", imported,
                "headers", headers,
                "errors", errors,
                "preview", valid.stream().limit(20).toList(),
                "committed", commit
        );
    }

    private boolean insertStudent(UUID tenant, Map<String,String> s) {
        UUID id = UUID.randomUUID();
        try {
            db.update("""
                insert into students(id,tenant_id,admission_number,first_name,last_name,email,phone,date_of_birth,gender,status,address)
                values(?,?,?,?,?,?,?,?,?,?,?)
                """, id, tenant, s.get("admissionNumber"), s.get("firstName"), nullable(s.get("lastName")),
                nullable(s.get("email")), nullable(s.get("phone")), parseDate(s.get("dateOfBirth")),
                nullable(s.get("gender")), Optional.ofNullable(nullable(s.get("status"))).orElse("ACTIVE"),
                nullable(s.get("address")));
            return true;
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new ApiException("IMPORT_CONFLICT", "A student with this admission number was created while the import was being processed");
        }
    }

    private List<String> validate(Map<String,String> s, UUID tenant) {
        List<String> errors = new ArrayList<>();
        for (String key : REQUIRED) if (nullable(s.get(key)) == null) errors.add(key + " is required");
        String admission = nullable(s.get("admissionNumber"));
        if (admission != null && db.queryForObject("select count(*) from students where tenant_id=? and admission_number=?", Integer.class, tenant, admission) > 0)
            errors.add("Admission number already exists");
        String dob = nullable(s.get("dateOfBirth"));
        if (dob != null) {
            try { LocalDate.parse(dob); } catch (Exception e) { errors.add("dateOfBirth must use YYYY-MM-DD"); }
        }
        String status = nullable(s.get("status"));
        if (status != null && !Set.of("ACTIVE","INACTIVE","WITHDRAWN","TRANSFERRED","ALUMNI").contains(status.toUpperCase()))
            errors.add("Unsupported student status");
        return errors;
    }

    private Map<String,String> canonicalize(Map<String,String> data, Map<String,String> mapping) {
        Map<String,String> out = new LinkedHashMap<>();
        Set<String> fields = Set.of("admissionNumber","firstName","lastName","email","phone","dateOfBirth","gender","status","address");
        for (String field : fields) {
            String source = mapping.getOrDefault(field, field);
            out.put(field, data.getOrDefault(normalizeHeader(source), ""));
        }
        return out;
    }

    private Map<String,String> parseMapping(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return mapper.readValue(json, new TypeReference<Map<String,String>>() {});
        } catch (Exception e) {
            throw new ApiException("VALIDATION_ERROR", "Invalid column mapping");
        }
    }

    private List<List<String>> parseCsv(String text) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < text.length() && text.charAt(i + 1) == '"') { cell.append('"'); i++; }
                else quoted = !quoted;
            } else if (ch == ',' && !quoted) { row.add(cell.toString()); cell.setLength(0); }
            else if ((ch == '\n' || ch == '\r') && !quoted) {
                if (ch == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                row.add(cell.toString()); cell.setLength(0);
                if (!row.isEmpty()) rows.add(row);
                row = new ArrayList<>();
            } else cell.append(ch);
        }
        if (cell.length() > 0 || !row.isEmpty()) { row.add(cell.toString()); rows.add(row); }
        return rows;
    }

    private String normalizeHeader(String value) {
        if (value == null) return "";
        return value.trim().replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
    }

    private LocalDate parseDate(String value) {
        String x = nullable(value);
        return x == null ? null : LocalDate.parse(x);
    }

    private String nullable(String value) {
        if (value == null) return null;
        String x = value.trim();
        return x.isEmpty() ? null : x;
    }

    private byte[] read(MultipartFile file) {
        try { return file.getBytes(); }
        catch (Exception e) { throw new ApiException("IMPORT_READ_ERROR", "Unable to read CSV file"); }
    }
}
