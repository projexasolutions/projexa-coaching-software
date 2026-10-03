package com.projexa.coaching.communication.controller;

import com.projexa.coaching.common.tenant.TenantContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/video")
public class VideoController {
  private final JdbcTemplate db;
  public VideoController(JdbcTemplate db){this.db=db;}

  @PostMapping("/sessions")
  @PreAuthorize("hasAuthority('communication.send')")
  public Map<String,Object> create(@RequestBody Session req,Authentication auth){
    UUID t=TenantContextHolder.getRequired(),host=UUID.fromString(auth.getName()),id=UUID.randomUUID();
    requireActiveUser(t,host);
    if(req.startsAt()==null) throw new IllegalArgumentException("Start time is required");
    if(req.endsAt()!=null&&!req.endsAt().isAfter(req.startsAt())) throw new IllegalArgumentException("End time must be after start time");
    String provider=req.provider()==null?"LOCAL":req.provider().trim().toUpperCase();
    if(!Set.of("LOCAL","ABSTRACT").contains(provider)) throw new IllegalArgumentException("Unsupported video provider");
    String room="projexa-"+UUID.randomUUID();
    db.update("insert into video_sessions(id,tenant_id,host_user_id,title,provider,external_room_id,starts_at,ends_at,status) values(?,?,?,?,?,?,?,?,?)",id,t,host,clean(req.title()),provider,room,req.startsAt(),req.endsAt(),"SCHEDULED");
    return Map.of("id",id,"roomId",room,"provider",provider);
  }

  @GetMapping("/sessions")
  public List<Map<String,Object>> list(Authentication auth){
    UUID t=TenantContextHolder.getRequired(),u=UUID.fromString(auth.getName());
    return db.queryForList("""
      select vs.* from video_sessions vs
      where vs.tenant_id=? and (vs.host_user_id=? or exists(select 1 from video_participants vp where vp.session_id=vs.id and vp.user_id=?))
      order by vs.starts_at desc
      """,t,u,u);
  }

  @PostMapping("/sessions/{id}/participants")
  @PreAuthorize("hasAuthority('communication.send')")
  public Map<String,Object> participant(@PathVariable UUID id,@RequestBody Map<String,String> req){
    UUID t=TenantContextHolder.getRequired(),u=UUID.fromString(req.get("userId")); requireSession(t,id); requireActiveUser(t,u);
    db.update("insert into video_participants(session_id,user_id,role) values(?,?,?) on conflict(session_id,user_id) do update set role=excluded.role",id,u,clean(req.get("role")));
    return Map.of("added",true);
  }

  @PostMapping("/sessions/{id}/status")
  @PreAuthorize("hasAuthority('communication.send')")
  public Map<String,Object> status(@PathVariable UUID id,@RequestBody Map<String,String> req){
    UUID t=TenantContextHolder.getRequired(); requireSession(t,id);
    String s=req.get("status"); if(s==null||!Set.of("SCHEDULED","LIVE","ENDED","CANCELLED").contains(s.toUpperCase())) throw new IllegalArgumentException("Invalid video status");
    db.update("update video_sessions set status=?,ends_at=case when ?='ENDED' then coalesce(ends_at,now()) else ends_at end where id=? and tenant_id=?",s.toUpperCase(),s.toUpperCase(),id,t);
    return Map.of("updated",true);
  }

  private void requireSession(UUID t,UUID id){if(count("select count(*) from video_sessions where id=? and tenant_id=?",id,t)==0)throw new IllegalArgumentException("Video session not found");}
  private void requireActiveUser(UUID t,UUID id){if(count("select count(*) from users where id=? and tenant_id=? and status='ACTIVE'",id,t)==0)throw new IllegalArgumentException("User is not active in this institute");}
  private String clean(String s){return s==null||s.isBlank()?null:s.trim();}
  private int count(String q,Object... a){Integer n=db.queryForObject(q,Integer.class,a);return n==null?0:n;}
  public record Session(String title,String provider,LocalDateTime startsAt,LocalDateTime endsAt){}
}