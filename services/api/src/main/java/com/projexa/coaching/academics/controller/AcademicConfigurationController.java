package com.projexa.coaching.academics.controller;

import com.projexa.coaching.common.responses.ApiResponse;
import com.projexa.coaching.common.tenant.TenantContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/academic-configuration")
public class AcademicConfigurationController {
    private final JdbcTemplate db;

    public AcademicConfigurationController(JdbcTemplate db) {
        this.db = db;
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('academics.read') or hasAuthority('dashboard.read') or hasAuthority('settings.manage')")
    public ApiResponse<Map<String,Object>> overview() {
        UUID tenant = TenantContextHolder.getRequired();
        Map<String,Object> out = new LinkedHashMap<>();
        out.put("academicYears", count("academic_years", tenant));
        out.put("classes", count("classes", tenant));
        out.put("streams", count("streams", tenant));
        out.put("subjects", count("subjects", tenant));
        out.put("batches", count("batches", tenant));
        out.put("classrooms", count("classrooms", tenant));
        out.put("classStreams", count("class_streams", tenant));
        out.put("classSubjects", count("class_subjects", tenant));
        return ApiResponse.ok(out);
    }

    @GetMapping("/class-streams")
    @PreAuthorize("hasAuthority('academics.read') or hasAuthority('dashboard.read') or hasAuthority('settings.manage')")
    public ApiResponse<List<Map<String,Object>>> classStreams() {
        UUID t = TenantContextHolder.getRequired();
        return ApiResponse.ok(db.queryForList("""
            select cs.id, cs.academic_year_id, cs.class_id, cs.stream_id,
                   ay.name academic_year_name, c.name class_name, s.name stream_name
            from class_streams cs
            join academic_years ay on ay.id=cs.academic_year_id and ay.tenant_id=cs.tenant_id
            join classes c on c.id=cs.class_id and c.tenant_id=cs.tenant_id
            join streams s on s.id=cs.stream_id and s.tenant_id=cs.tenant_id
            where cs.tenant_id=? order by ay.start_date desc, c.display_order, c.name, s.name
        """, t));
    }

    @PostMapping("/class-streams")
    @PreAuthorize("hasAuthority('academics.manage') or hasAuthority('settings.manage')")
    public ApiResponse<Map<String,Object>> addClassStream(@RequestBody Map<String,String> p) {
        UUID t = TenantContextHolder.getRequired();
        UUID year = uuid(p,"academicYearId"), clazz = uuid(p,"classId"), stream = uuid(p,"streamId");
        require("academic_years",year,t,"Academic year"); require("classes",clazz,t,"Class"); require("streams",stream,t,"Stream");
        Integer exists = db.queryForObject("select count(*) from class_streams where tenant_id=? and academic_year_id=? and class_id=? and stream_id=?", Integer.class,t,year,clazz,stream);
        if (exists != null && exists > 0) throw new IllegalArgumentException("This stream is already assigned to the class for this academic year.");
        UUID id=UUID.randomUUID();
        db.update("insert into class_streams(id,tenant_id,academic_year_id,class_id,stream_id) values(?,?,?,?,?)",id,t,year,clazz,stream);
        return ApiResponse.ok(Map.of("id",id));
    }

    @DeleteMapping("/class-streams/{id}")
    @PreAuthorize("hasAuthority('academics.manage') or hasAuthority('settings.manage')")
    public ApiResponse<Void> deleteClassStream(@PathVariable UUID id) {
        UUID t=TenantContextHolder.getRequired();
        int n=db.update("delete from class_streams where id=? and tenant_id=?",id,t);
        if(n==0) throw new IllegalArgumentException("Class-stream mapping not found.");
        return ApiResponse.ok(null);
    }

    @GetMapping("/class-subjects")
    @PreAuthorize("hasAuthority('academics.read') or hasAuthority('dashboard.read') or hasAuthority('settings.manage')")
    public ApiResponse<List<Map<String,Object>>> classSubjects() {
        UUID t=TenantContextHolder.getRequired();
        return ApiResponse.ok(db.queryForList("""
            select cs.id, cs.academic_year_id, cs.class_id, cs.subject_id,
                   ay.name academic_year_name, c.name class_name, s.name subject_name, s.code subject_code
            from class_subjects cs
            join academic_years ay on ay.id=cs.academic_year_id and ay.tenant_id=cs.tenant_id
            join classes c on c.id=cs.class_id and c.tenant_id=cs.tenant_id
            join subjects s on s.id=cs.subject_id and s.tenant_id=cs.tenant_id
            where cs.tenant_id=? order by ay.start_date desc, c.display_order, c.name, s.name
        """,t));
    }

    @PostMapping("/class-subjects")
    @PreAuthorize("hasAuthority('academics.manage') or hasAuthority('settings.manage')")
    public ApiResponse<Map<String,Object>> addClassSubject(@RequestBody Map<String,String> p) {
        UUID t=TenantContextHolder.getRequired();
        UUID year=uuid(p,"academicYearId"), clazz=uuid(p,"classId"), subject=uuid(p,"subjectId");
        require("academic_years",year,t,"Academic year"); require("classes",clazz,t,"Class"); require("subjects",subject,t,"Subject");
        Integer exists=db.queryForObject("select count(*) from class_subjects where tenant_id=? and academic_year_id=? and class_id=? and subject_id=?",Integer.class,t,year,clazz,subject);
        if(exists!=null&&exists>0) throw new IllegalArgumentException("This subject is already assigned to the class for this academic year.");
        UUID id=UUID.randomUUID();
        db.update("insert into class_subjects(id,tenant_id,academic_year_id,class_id,subject_id) values(?,?,?,?,?)",id,t,year,clazz,subject);
        return ApiResponse.ok(Map.of("id",id));
    }

    @DeleteMapping("/class-subjects/{id}")
    @PreAuthorize("hasAuthority('academics.manage') or hasAuthority('settings.manage')")
    public ApiResponse<Void> deleteClassSubject(@PathVariable UUID id) {
        UUID t=TenantContextHolder.getRequired();
        int n=db.update("delete from class_subjects where id=? and tenant_id=?",id,t);
        if(n==0) throw new IllegalArgumentException("Class-subject mapping not found.");
        return ApiResponse.ok(null);
    }

    @GetMapping("/classrooms")
    @PreAuthorize("hasAuthority('academics.read') or hasAuthority('dashboard.read') or hasAuthority('settings.manage')")
    public ApiResponse<List<Map<String,Object>>> classrooms() {
        UUID t=TenantContextHolder.getRequired();
        return ApiResponse.ok(db.queryForList("select id,name,room_code,capacity,active from classrooms where tenant_id=? order by name",t));
    }

    @PostMapping("/classrooms")
    @PreAuthorize("hasAuthority('academics.manage') or hasAuthority('settings.manage')")
    public ApiResponse<Map<String,Object>> createClassroom(@RequestBody Map<String,Object> p) {
        UUID t=TenantContextHolder.getRequired();
        String name=String.valueOf(p.getOrDefault("name","")).trim();
        if(name.isBlank()) throw new IllegalArgumentException("Classroom name is required.");
        String code=nullable(p.get("roomCode")); Integer capacity=number(p.get("capacity"));
        UUID id=UUID.randomUUID();
        db.update("insert into classrooms(id,tenant_id,name,room_code,capacity,active) values(?,?,?,?,?,?)",id,t,name,code,capacity,booleanValue(p.get("active"),true));
        return ApiResponse.ok(Map.of("id",id));
    }

    @PutMapping("/classrooms/{id}")
    @PreAuthorize("hasAuthority('academics.manage') or hasAuthority('settings.manage')")
    public ApiResponse<Void> updateClassroom(@PathVariable UUID id,@RequestBody Map<String,Object> p) {
        UUID t=TenantContextHolder.getRequired();
        String name=String.valueOf(p.getOrDefault("name","")).trim();
        if(name.isBlank()) throw new IllegalArgumentException("Classroom name is required.");
        int n=db.update("update classrooms set name=?,room_code=?,capacity=?,active=? where id=? and tenant_id=?",name,nullable(p.get("roomCode")),number(p.get("capacity")),booleanValue(p.get("active"),true),id,t);
        if(n==0) throw new IllegalArgumentException("Classroom not found.");
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/classrooms/{id}")
    @PreAuthorize("hasAuthority('academics.manage') or hasAuthority('settings.manage')")
    public ApiResponse<Void> deleteClassroom(@PathVariable UUID id) {
        UUID t=TenantContextHolder.getRequired();
        Integer refs=db.queryForObject("select count(*) from timetable_entries where classroom_id=? and tenant_id=?",Integer.class,id,t);
        if(refs!=null&&refs>0) throw new IllegalArgumentException("This classroom is used by timetable entries and cannot be deleted.");
        int n=db.update("delete from classrooms where id=? and tenant_id=?",id,t);
        if(n==0) throw new IllegalArgumentException("Classroom not found.");
        return ApiResponse.ok(null);
    }

    private int count(String table,UUID tenant){Integer n=db.queryForObject("select count(*) from "+table+" where tenant_id=?",Integer.class,tenant);return n==null?0:n;}
    private void require(String table,UUID id,UUID tenant,String label){Integer n=db.queryForObject("select count(*) from "+table+" where id=? and tenant_id=?",Integer.class,id,tenant);if(n==null||n==0)throw new IllegalArgumentException(label+" not found.");}
    private UUID uuid(Map<String,String> p,String key){try{return UUID.fromString(String.valueOf(p.get(key)));}catch(Exception e){throw new IllegalArgumentException(key+" is required.");}}
    private String nullable(Object x){if(x==null)return null;String s=String.valueOf(x).trim();return s.isBlank()?null:s;}
    private Integer number(Object x){if(x==null||String.valueOf(x).isBlank())return null;try{return Integer.valueOf(String.valueOf(x));}catch(Exception e){throw new IllegalArgumentException("Capacity must be a number.");}}
    private boolean booleanValue(Object x,boolean d){if(x==null)return d;return Boolean.parseBoolean(String.valueOf(x));}
}
