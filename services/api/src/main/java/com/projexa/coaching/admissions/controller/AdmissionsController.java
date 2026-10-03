package com.projexa.coaching.admissions.controller;

import com.projexa.coaching.common.tenant.TenantContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/admissions")
public class AdmissionsController {
  private final JdbcTemplate db;
  public AdmissionsController(JdbcTemplate db){this.db=db;}

  @GetMapping("/leads")
  @PreAuthorize("hasAuthority('admissions.manage')")
  public List<Map<String,Object>> list(@RequestParam(required=false) String stage,@RequestParam(required=false) String search){
    UUID t=TenantContextHolder.getRequired(); String s=search==null?null:search.trim().toLowerCase();
    if(stage!=null&&!validStage(stage)) throw new IllegalArgumentException("Invalid lead stage");
    return db.queryForList("""
      select l.*,u.email counsellor_email,
      (select count(*) from lead_activities a where a.tenant_id=l.tenant_id and a.lead_id=l.id) activity_count,
      (select min(f.due_at) from follow_ups f where f.tenant_id=l.tenant_id and f.lead_id=l.id and f.status='PENDING') next_follow_up
      from leads l left join users u on u.id=l.counsellor_user_id and u.tenant_id=l.tenant_id
      where l.tenant_id=? and (? is null or l.stage=?) and
      (? is null or lower(l.name) like ? or lower(coalesce(l.phone,'')) like ? or lower(coalesce(l.email,'')) like ?)
      order by l.updated_at desc
      """,t,stage,stage,s,s==null?null:"%"+s+"%",s==null?null:"%"+s+"%",s==null?null:"%"+s+"%");
  }

  @GetMapping("/funnel")
  @PreAuthorize("hasAuthority('admissions.manage') or hasAuthority('reports.read')")
  public Map<String,Object> funnel(){
    UUID t=TenantContextHolder.getRequired();
    return Map.of("stages",db.queryForList("select stage,count(*) count from leads where tenant_id=? group by stage order by stage",t),
      "sources",db.queryForList("select coalesce(source,'Unknown') source,count(*) count from leads where tenant_id=? group by source order by count desc",t),
      "conversion",db.queryForMap("select count(*) total,count(*) filter(where stage='ADMITTED') admitted,count(*) filter(where stage='LOST') lost from leads where tenant_id=?",t));
  }

  @GetMapping("/leads/{id}")
  @PreAuthorize("hasAuthority('admissions.manage')")
  public Map<String,Object> get(@PathVariable UUID id){
    UUID t=TenantContextHolder.getRequired(); return detail(t,id);
  }

  private Map<String,Object> detail(UUID t,UUID id){
    requireLead(t,id);
    return Map.of("lead",db.queryForMap("select * from leads where id=? and tenant_id=?",id,t),
      "activities",db.queryForList("select * from lead_activities where tenant_id=? and lead_id=? order by created_at desc",t,id),
      "followUps",db.queryForList("select * from follow_ups where tenant_id=? and lead_id=? order by due_at asc",t,id));
  }

  @PostMapping("/leads")
  @PreAuthorize("hasAuthority('admissions.manage')")
  public Map<String,Object> create(@RequestBody Lead r){
    UUID t=TenantContextHolder.getRequired(); validate(r); UUID id=UUID.randomUUID();
    validateCounsellor(t,r.counsellorUserId());
    db.update("insert into leads(id,tenant_id,name,phone,email,source,stage,counsellor_user_id,lost_reason) values(?,?,?,?,?,?,?,?,?)",
      id,t,r.name().trim(),clean(r.phone()),clean(r.email()),clean(r.source()),stage(r.stage()),r.counsellorUserId(),clean(r.lostReason()));
    addActivity(t,id,r.counsellorUserId(),"CREATED","Lead created"); return detail(t,id);
  }

  @PutMapping("/leads/{id}")
  @PreAuthorize("hasAuthority('admissions.manage')")
  public Map<String,Object> update(@PathVariable UUID id,@RequestBody Lead r){
    UUID t=TenantContextHolder.getRequired(); requireLead(t,id); validate(r); validateCounsellor(t,r.counsellorUserId());
    db.update("update leads set name=?,phone=?,email=?,source=?,stage=?,counsellor_user_id=?,lost_reason=?,updated_at=now() where id=? and tenant_id=?",
      r.name().trim(),clean(r.phone()),clean(r.email()),clean(r.source()),stage(r.stage()),r.counsellorUserId(),clean(r.lostReason()),id,t);
    addActivity(t,id,r.counsellorUserId(),"UPDATED","Lead details updated"); return detail(t,id);
  }

  @PostMapping("/leads/{id}/stage")
  @PreAuthorize("hasAuthority('admissions.manage')")
  public Map<String,Object> move(@PathVariable UUID id,@RequestBody Map<String,String> req){
    UUID t=TenantContextHolder.getRequired(); requireLead(t,id); String next=stage(req.get("stage"));
    String reason=clean(req.get("lostReason"));
    if("LOST".equals(next)&&reason==null) throw new IllegalArgumentException("Lost reason is required");
    db.update("update leads set stage=?,lost_reason=?,updated_at=now() where id=? and tenant_id=?",next,reason,id,t);
    addActivity(t,id,null,"STAGE_CHANGED","Moved to "+next); return detail(t,id);
  }

  @PostMapping("/leads/{id}/activities")
  @PreAuthorize("hasAuthority('admissions.manage')")
  public Map<String,Object> activity(@PathVariable UUID id,@RequestBody Activity r){
    UUID t=TenantContextHolder.getRequired(); requireLead(t,id);
    if(r.type()==null||r.type().isBlank()) throw new IllegalArgumentException("Activity type is required");
    addActivity(t,id,r.userId(),r.type().trim().toUpperCase(),r.notes()); return Map.of("created",true);
  }

  @PostMapping("/leads/{id}/follow-ups")
  @PreAuthorize("hasAuthority('admissions.manage')")
  public Map<String,Object> followUp(@PathVariable UUID id,@RequestBody FollowUp r){
    UUID t=TenantContextHolder.getRequired(); requireLead(t,id);
    if(r.dueAt()==null||r.dueAt().isBefore(LocalDateTime.now())) throw new IllegalArgumentException("Follow-up must be scheduled in the future");
    validateCounsellor(t,r.assignedUserId()); UUID fid=UUID.randomUUID();
    db.update("insert into follow_ups(id,tenant_id,lead_id,assigned_user_id,due_at,status,notes) values(?,?,?,?,?,'PENDING',?)",fid,t,id,r.assignedUserId(),r.dueAt(),r.notes());
    return Map.of("id",fid,"created",true);
  }

  @PostMapping("/follow-ups/{id}/complete")
  @PreAuthorize("hasAuthority('admissions.manage')")
  public Map<String,Object> complete(@PathVariable UUID id){
    UUID t=TenantContextHolder.getRequired();
    int n=db.update("update follow_ups set status='COMPLETED' where id=? and tenant_id=? and status='PENDING'",id,t);
    if(n==0) throw new IllegalArgumentException("Follow-up not found or already completed");
    return Map.of("completed",true);
  }

  private void addActivity(UUID t,UUID leadId,UUID userId,String type,String notes){db.update("insert into lead_activities(id,tenant_id,lead_id,user_id,type,notes) values(?,?,?,?,?,?)",UUID.randomUUID(),t,leadId,userId,type,notes);}
  private void requireLead(UUID t,UUID id){if(count("select count(*) from leads where id=? and tenant_id=?",id,t)==0)throw new IllegalArgumentException("Lead not found");}
  private void validate(Lead r){if(r==null||r.name()==null||r.name().trim().isEmpty())throw new IllegalArgumentException("Lead name is required");}
  private void validateCounsellor(UUID t,UUID id){if(id!=null&&count("select count(*) from users where id=? and tenant_id=? and status='ACTIVE'",id,t)==0)throw new IllegalArgumentException("Assigned user is invalid");}
  private String stage(String s){String v=s==null?"ENQUIRY":s.trim().toUpperCase();if(!validStage(v))throw new IllegalArgumentException("Invalid lead stage");return v;}
  private boolean validStage(String s){return Set.of("ENQUIRY","COUNSELLING","DEMO","FOLLOW_UP","ADMITTED","LOST").contains(s.trim().toUpperCase());}
  private String clean(String s){return s==null||s.isBlank()?null:s.trim();}
  private int count(String sql,Object... args){Integer n=db.queryForObject(sql,Integer.class,args);return n==null?0:n;}
  public record Lead(String name,String phone,String email,String source,String stage,UUID counsellorUserId,String lostReason){}
  public record Activity(UUID userId,String type,String notes){}
  public record FollowUp(UUID assignedUserId,LocalDateTime dueAt,String notes){}
}