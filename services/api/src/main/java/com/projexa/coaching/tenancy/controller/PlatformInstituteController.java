package com.projexa.coaching.tenancy.controller;

import com.projexa.coaching.common.responses.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/platform/institutes")
@PreAuthorize("hasAuthority('platform.institutes.read')")
public class PlatformInstituteController {
    private final JdbcTemplate db;
    public PlatformInstituteController(JdbcTemplate db){this.db=db;}

    @GetMapping
    public ApiResponse<List<Map<String,Object>>> list(){
        return ApiResponse.ok(db.queryForList("""
            select t.id,t.name,t.slug,t.status,t.contact_email,t.contact_phone,t.timezone,
                   p.setup_status,p.current_step,p.go_live_at,
                   coalesce((select count(*) from students s where s.tenant_id=t.id),0) students,
                   coalesce((select count(*) from programs pr where pr.tenant_id=t.id),0) programs,
                   coalesce((select count(*) from batches b where b.tenant_id=t.id),0) batches
            from tenants t
            left join institute_setup_profiles p on p.tenant_id=t.id
            order by t.created_at desc
        """));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String,Object>> get(@PathVariable UUID id){
        return ApiResponse.ok(detail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('platform.institutes.manage')")
    public ApiResponse<Map<String,Object>> create(@RequestBody Map<String,Object> p, Authentication auth){
        String name=text(p.get("name"));
        String slug=text(p.get("slug"));
        if(name==null||name.length()>150) throw new IllegalArgumentException("Institute name is required.");
        if(slug==null||!slug.matches("[a-z0-9-]{2,100}")) throw new IllegalArgumentException("Slug must contain 2-100 lowercase letters, numbers or hyphens.");
        Integer exists=db.queryForObject("select count(*) from tenants where lower(slug)=lower(?)",Integer.class,slug);
        if(exists!=null&&exists>0) throw new IllegalArgumentException("Institute slug already exists.");

        UUID tenant=UUID.randomUUID();
        db.update("""
          insert into tenants(id,name,slug,status,timezone,contact_email,contact_phone,address)
          values(?,?,?,?,?,?,?,?)
        """,tenant,name,slug,"ACTIVE",nullable(p.get("timezone"),"Asia/Kolkata"),
           nullable(p.get("contactEmail"),null),nullable(p.get("contactPhone"),null),nullable(p.get("address"),null));
        db.update("insert into institute_setup_profiles(tenant_id,institute_type) values(?,?)",
           tenant,nullable(p.get("instituteType"),"OTHER"));

        UUID admin=UUID.fromString(auth.getName());
        audit(admin,"INSTITUTE_CREATED",tenant,Map.of("name",name,"slug",slug));
        return ApiResponse.ok(Map.of("id",tenant));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('platform.institutes.manage')")
    public ApiResponse<Void> status(@PathVariable UUID id,@RequestBody Map<String,Object> p,Authentication auth){
        String status=text(p.get("status"));
        if(!Set.of("ACTIVE","SUSPENDED","ARCHIVED").contains(status)) throw new IllegalArgumentException("Invalid institute status.");
        int n=db.update("update tenants set status=?,updated_at=current_timestamp where id=?",status,id);
        if(n==0) throw new IllegalArgumentException("Institute not found.");
        audit(UUID.fromString(auth.getName()),"INSTITUTE_STATUS_CHANGED",id,Map.of("status",status));
        return ApiResponse.ok(null);
    }

    private Map<String,Object> detail(UUID id){
        Map<String,Object> row;
        try{
            row=db.queryForMap("""
              select t.*,p.setup_status,p.current_step,p.completed_steps,p.notes,p.go_live_at
              from tenants t left join institute_setup_profiles p on p.tenant_id=t.id where t.id=?
            """,id);
        }catch(Exception e){throw new IllegalArgumentException("Institute not found.");}
        return row;
    }
    private void audit(UUID admin,String action,UUID tenant,Map<String,Object> metadata){
        db.update("insert into platform_admin_audit(admin_user_id,action,tenant_id,metadata) values(?,?,?,?::jsonb)",
          admin,action,tenant, new org.springframework.jdbc.support.json.SqlJsonValue(
            new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(metadata).toString()));
    }
    private String text(Object x){return nullable(x,null);}
    private String nullable(Object x,String d){if(x==null)return d;String s=String.valueOf(x).trim();return s.isBlank()?d:s;}
}