package com.projexa.coaching.social.controller;

import com.projexa.coaching.common.tenant.TenantContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/social-studio")
@PreAuthorize("hasAuthority('social.manage')")
public class SocialStudioController {
    private final JdbcTemplate db;
    public SocialStudioController(JdbcTemplate db) { this.db = db; }

    @GetMapping("/posts")
    public List<Map<String,Object>> list() {
        return db.queryForList("select * from social_posts where tenant_id=? order by coalesce(scheduled_for,created_at) asc, created_at desc", TenantContextHolder.getRequired());
    }

    @PostMapping("/posts")
    public Map<String,Object> create(@RequestBody PostRequest req) {
        validate(req);
        UUID id=UUID.randomUUID(), tenant=TenantContextHolder.getRequired();
        db.update("""
            insert into social_posts(id,tenant_id,title,category,platforms,format,caption,hashtags,scheduled_for,status)
            values(?,?,?,?,?,?,?,?,?,?)
            """, id,tenant,req.title().trim(),req.category(),req.platforms(),req.format(),req.caption().trim(),clean(req.hashtags()),parseDate(req.scheduledFor()),status(req.status()));
        return find(id,tenant);
    }

    @PutMapping("/posts/{id}")
    public Map<String,Object> update(@PathVariable UUID id,@RequestBody PostRequest req) {
        validate(req);
        UUID tenant=TenantContextHolder.getRequired();
        int updated=db.update("""
            update social_posts set title=?,category=?,platforms=?,format=?,caption=?,hashtags=?,scheduled_for=?,status=?,updated_at=now()
            where id=? and tenant_id=?
            """,req.title().trim(),req.category(),req.platforms(),req.format(),req.caption().trim(),clean(req.hashtags()),parseDate(req.scheduledFor()),status(req.status()),id,tenant);
        if(updated==0) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Post not found");
        return find(id,tenant);
    }

    @DeleteMapping("/posts/{id}")
    public Map<String,Object> delete(@PathVariable UUID id) {
        int deleted=db.update("delete from social_posts where id=? and tenant_id=?",id,TenantContextHolder.getRequired());
        if(deleted==0) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Post not found");
        return Map.of("deleted",true);
    }

    private Map<String,Object> find(UUID id,UUID tenant) {
        return db.queryForMap("select * from social_posts where id=? and tenant_id=?",id,tenant);
    }
    private void validate(PostRequest r) {
        if(r==null||r.title()==null||r.title().isBlank()||r.title().length()>180) throw new IllegalArgumentException("A post title (max 180 characters) is required");
        if(r.caption()==null||r.caption().isBlank()) throw new IllegalArgumentException("Post caption is required");
        if(r.category()==null||!Set.of("EDUCATIONAL","PROMOTIONAL","ENGAGEMENT","BRANDING").contains(r.category())) throw new IllegalArgumentException("Invalid content category");
        if(r.platforms()==null||!Set.of("INSTAGRAM","FACEBOOK","INSTAGRAM,FACEBOOK").contains(r.platforms())) throw new IllegalArgumentException("Invalid social platforms");
        if(r.format()==null||r.format().isBlank()||r.format().length()>40) throw new IllegalArgumentException("A valid post format is required");
    }
    private String status(String value) {
        String s=value==null?"DRAFT":value.toUpperCase(Locale.ROOT);
        if(!Set.of("DRAFT","APPROVED","SCHEDULED","PUBLISHED").contains(s)) throw new IllegalArgumentException("Invalid post status");
        return s;
    }
    private LocalDateTime parseDate(String value) { return value==null||value.isBlank()?null:LocalDateTime.parse(value.length()>19?value.substring(0,19):value); }
    private String clean(String value) { return value==null||value.isBlank()?null:value.trim(); }
    public record PostRequest(String title,String category,String platforms,String format,String caption,String hashtags,String scheduledFor,String status) {}
}
