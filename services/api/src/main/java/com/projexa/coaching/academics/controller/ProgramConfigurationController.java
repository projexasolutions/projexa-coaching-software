package com.projexa.coaching.academics.controller;

import com.projexa.coaching.common.responses.ApiResponse;
import com.projexa.coaching.common.tenant.TenantContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/programs")
public class ProgramConfigurationController {
    private final JdbcTemplate db;
    public ProgramConfigurationController(JdbcTemplate db){this.db=db;}

    @GetMapping
    @PreAuthorize("hasAuthority('programs.read') or hasAuthority('academics.read') or hasAuthority('dashboard.read')")
    public ApiResponse<List<Map<String,Object>>> list(){
        UUID t=TenantContextHolder.getRequired();
        return ApiResponse.ok(db.queryForList("""
          select p.id,p.name,p.code,p.category,p.level,p.description,p.active,p.program_id
          from programs p where p.tenant_id=? order by p.active desc,p.name
        """.replace(",p.program_id",""),t));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('programs.manage') or hasAuthority('academics.manage') or hasAuthority('settings.manage')")
    public ApiResponse<Map<String,Object>> create(@RequestBody Map<String,Object> p){
        UUID t=TenantContextHolder.getRequired();
        String name=text(p,"name","Program name is required");
        String code=nullable(p.get("code")), category=textDefault(p,"category","GENERAL"), level=nullable(p.get("level"));
        String description=nullable(p.get("description"));
        Integer dup=db.queryForObject("select count(*) from programs where tenant_id=? and lower(name)=lower(?)",Integer.class,t,name);
        if(dup!=null&&dup>0) throw new IllegalArgumentException("A program with this name already exists.");
        UUID id=UUID.randomUUID();
        db.update("insert into programs(id,tenant_id,name,code,category,level,description,active) values(?,?,?,?,?,?,?,?)",
          id,t,name,code,category,level,description,booleanValue(p.get("active"),true));
        return ApiResponse.ok(Map.of("id",id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('programs.manage') or hasAuthority('academics.manage') or hasAuthority('settings.manage')")
    public ApiResponse<Void> update(@PathVariable UUID id,@RequestBody Map<String,Object> p){
        UUID t=TenantContextHolder.getRequired();
        String name=text(p,"name","Program name is required");
        int n=db.update("update programs set name=?,code=?,category=?,level=?,description=?,active=?,updated_at=current_timestamp where id=? and tenant_id=?",
          name,nullable(p.get("code")),textDefault(p,"category","GENERAL"),nullable(p.get("level")),nullable(p.get("description")),booleanValue(p.get("active"),true),id,t);
        if(n==0) throw new IllegalArgumentException("Program not found.");
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('programs.manage') or hasAuthority('academics.manage') or hasAuthority('settings.manage')")
    public ApiResponse<Void> delete(@PathVariable UUID id){
        UUID t=TenantContextHolder.getRequired();
        Integer refs=db.queryForObject("select count(*) from batches where tenant_id=? and program_id=?",Integer.class,t,id);
        if(refs!=null&&refs>0) throw new IllegalArgumentException("This program is linked to batches. Reassign those batches before deleting.");
        int n=db.update("delete from programs where id=? and tenant_id=?",id,t);
        if(n==0) throw new IllegalArgumentException("Program not found.");
        return ApiResponse.ok(null);
    }

    @GetMapping("/{id}/subjects")
    @PreAuthorize("hasAuthority('programs.read') or hasAuthority('academics.read') or hasAuthority('dashboard.read')")
    public ApiResponse<List<Map<String,Object>>> subjects(@PathVariable UUID id){
        UUID t=TenantContextHolder.getRequired(); require("programs",id,t,"Program");
        return ApiResponse.ok(db.queryForList("""
          select ps.id,ps.program_id,ps.subject_id,s.name subject_name,s.code subject_code,ps.display_order,ps.active
          from program_subjects ps join subjects s on s.id=ps.subject_id and s.tenant_id=ps.tenant_id
          where ps.tenant_id=? and ps.program_id=? order by ps.display_order,s.name
        """,t,id));
    }

    @PostMapping("/{id}/subjects")
    @PreAuthorize("hasAuthority('programs.manage') or hasAuthority('academics.manage') or hasAuthority('settings.manage')")
    public ApiResponse<Map<String,Object>> addSubject(@PathVariable UUID id,@RequestBody Map<String,Object> p){
        UUID t=TenantContextHolder.getRequired(), subject=uuid(p.get("subjectId"),"subjectId");
        require("programs",id,t,"Program"); require("subjects",subject,t,"Subject");
        Integer dup=db.queryForObject("select count(*) from program_subjects where tenant_id=? and program_id=? and subject_id=?",Integer.class,t,id,subject);
        if(dup!=null&&dup>0) throw new IllegalArgumentException("Subject is already assigned to this program.");
        UUID row=UUID.randomUUID(); int order=number(p.get("displayOrder"),0);
        db.update("insert into program_subjects(id,tenant_id,program_id,subject_id,display_order,active) values(?,?,?,?,?,?)",row,t,id,subject,order,booleanValue(p.get("active"),true));
        return ApiResponse.ok(Map.of("id",row));
    }

    @DeleteMapping("/{id}/subjects/{rowId}")
    @PreAuthorize("hasAuthority('programs.manage') or hasAuthority('academics.manage') or hasAuthority('settings.manage')")
    public ApiResponse<Void> removeSubject(@PathVariable UUID id,@PathVariable UUID rowId){
        UUID t=TenantContextHolder.getRequired();
        int n=db.update("delete from program_subjects where id=? and program_id=? and tenant_id=?",rowId,id,t);
        if(n==0) throw new IllegalArgumentException("Program subject mapping not found.");
        return ApiResponse.ok(null);
    }

    @GetMapping("/templates")
    @PreAuthorize("hasAuthority('programs.read') or hasAuthority('academics.read') or hasAuthority('dashboard.read')")
    public ApiResponse<List<Map<String,Object>>> templates(){
        UUID t=TenantContextHolder.getRequired();
        return ApiResponse.ok(db.queryForList("""
          select id,name,code,category,description,config,system_template,active
          from exam_templates where active=true and (tenant_id is null or tenant_id=?)
          order by system_template desc,category,name
        """,t));
    }

    private void require(String table,UUID id,UUID t,String label){Integer n=db.queryForObject("select count(*) from "+table+" where id=? and tenant_id=?",Integer.class,id,t);if(n==null||n==0)throw new IllegalArgumentException(label+" not found.");}
    private String text(Map<String,Object> p,String k,String msg){String s=nullable(p.get(k));if(s==null)throw new IllegalArgumentException(msg);return s;}
    private String textDefault(Map<String,Object> p,String k,String d){String s=nullable(p.get(k));return s==null?d:s.toUpperCase();}
    private String nullable(Object x){if(x==null)return null;String s=String.valueOf(x).trim();return s.isBlank()?null:s;}
    private UUID uuid(Object x,String k){try{return UUID.fromString(String.valueOf(x));}catch(Exception e){throw new IllegalArgumentException(k+" is required.");}}
    private int number(Object x,int d){if(x==null||String.valueOf(x).isBlank())return d;try{return Integer.parseInt(String.valueOf(x));}catch(Exception e){throw new IllegalArgumentException("Display order must be a number.");}}
    private boolean booleanValue(Object x,boolean d){return x==null?d:Boolean.parseBoolean(String.valueOf(x));}
}