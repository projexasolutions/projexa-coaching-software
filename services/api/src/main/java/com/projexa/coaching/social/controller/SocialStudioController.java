package com.projexa.coaching.social.controller;

import com.projexa.coaching.common.tenant.TenantContextHolder;
import com.projexa.coaching.social.service.SocialContentAiService;
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
    private final SocialContentAiService ai;
    public SocialStudioController(JdbcTemplate db, SocialContentAiService ai) { this.db = db; this.ai = ai; }

    @PostMapping("/ai/caption")
    public Map<String,String> generateCaption(@RequestBody CaptionRequest req) {
        if(req==null||req.title()==null||req.title().isBlank()) throw new IllegalArgumentException("Post title is required");
        return ai.generate(req.title().trim(),req.category(),req.format(),req.instituteName());
    }

    @GetMapping("/posts")
    public List<Map<String,Object>> list() {
        return db.queryForList("select * from social_posts where tenant_id=? order by coalesce(scheduled_for,created_at) asc, created_at desc", TenantContextHolder.getRequired());
    }

    @PostMapping("/posts")
    public Map<String,Object> create(@RequestBody PostRequest req) {
        validate(req);
        String initialStatus=status(req.status());
        if(!"DRAFT".equals(initialStatus)) throw new IllegalArgumentException("New posts must be created as drafts and approved before scheduling");
        UUID id=UUID.randomUUID(), tenant=TenantContextHolder.getRequired();
        db.update("""
            insert into social_posts(id,tenant_id,title,category,platforms,format,caption,hashtags,scheduled_for,status)
            values(?,?,?,?,?,?,?,?,?,?)
            """, id,tenant,req.title().trim(),req.category(),req.platforms(),req.format(),req.caption().trim(),clean(req.hashtags()),parseDate(req.scheduledFor()),initialStatus);
        return find(id,tenant);
    }

    @PutMapping("/posts/{id}")
    public Map<String,Object> update(@PathVariable UUID id,@RequestBody PostRequest req) {
        validate(req);
        UUID tenant=TenantContextHolder.getRequired();
        String nextStatus=status(req.status());
        LocalDateTime scheduledAt=parseDate(req.scheduledFor());
        Map<String,Object> existing=find(id,tenant);
        validateTransition(String.valueOf(existing.get("status")),nextStatus,scheduledAt);
        int updated=db.update("""
            update social_posts set title=?,category=?,platforms=?,format=?,caption=?,hashtags=?,scheduled_for=?,status=?,updated_at=now()
            where id=? and tenant_id=?
            """,req.title().trim(),req.category(),req.platforms(),req.format(),req.caption().trim(),clean(req.hashtags()),scheduledAt,nextStatus,id,tenant);
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
    private void validateTransition(String current,String next,LocalDateTime scheduledAt) {
        if("PUBLISHED".equals(next)) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"Posts can only be marked published after a connected publishing provider confirms success");
        if("SCHEDULED".equals(next)) {
            if(scheduledAt==null) throw new IllegalArgumentException("Choose a planned date and time before scheduling");
            if(!scheduledAt.isAfter(LocalDateTime.now())) throw new IllegalArgumentException("Scheduled date and time must be in the future");
            if(!"APPROVED".equals(current)&&!"SCHEDULED".equals(current)) throw new IllegalArgumentException("Approve the draft before adding it to the publishing queue");
        }
    }
    private String status(String value) {
        String s=value==null?"DRAFT":value.toUpperCase(Locale.ROOT);
        if(!Set.of("DRAFT","APPROVED","SCHEDULED","PUBLISHED").contains(s)) throw new IllegalArgumentException("Invalid post status");
        return s;
    }
    private LocalDateTime parseDate(String value) { return value==null||value.isBlank()?null:LocalDateTime.parse(value.length()>19?value.substring(0,19):value); }
    private String clean(String value) { return value==null||value.isBlank()?null:value.trim(); }
    public record PostRequest(String title,String category,String platforms,String format,String caption,String hashtags,String scheduledFor,String status) {}
    public record CaptionRequest(String title,String category,String format,String instituteName) {}
}
