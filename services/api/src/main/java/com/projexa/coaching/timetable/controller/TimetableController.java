package com.projexa.coaching.timetable.controller;

import com.projexa.coaching.common.tenant.TenantContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/timetable")
public class TimetableController {
  private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F-]{36}$");
  private final JdbcTemplate db;

  public TimetableController(JdbcTemplate db){this.db=db;}

  @GetMapping
  @PreAuthorize("hasAuthority('timetable.manage') or hasAuthority('dashboard.read')")
  public List<Map<String,Object>> list(@RequestParam(required=false) UUID batchId,
                                        @RequestParam(required=false) Integer dayOfWeek){
    UUID t=TenantContextHolder.getRequired();
    StringBuilder sql=new StringBuilder("""
      select te.id,te.batch_id,te.subject_id,te.teacher_id,te.classroom_id,
             te.day_of_week,te.start_time,te.end_time,
             b.name batch_name,coalesce(s.name,'General') subject_name,
             concat_ws(' ',th.first_name,th.last_name) teacher_name,
             c.name classroom_name
      from timetable_entries te
      join batches b on b.id=te.batch_id and b.tenant_id=te.tenant_id
      left join subjects s on s.id=te.subject_id and s.tenant_id=te.tenant_id
      left join teachers th on th.id=te.teacher_id and th.tenant_id=te.tenant_id
      left join classrooms c on c.id=te.classroom_id and c.tenant_id=te.tenant_id
      where te.tenant_id=?
      """);
    List<Object> args=new ArrayList<>(); args.add(t);
    if(batchId!=null){sql.append(" and te.batch_id=?");args.add(batchId);}
    if(dayOfWeek!=null){validateDay(dayOfWeek);sql.append(" and te.day_of_week=?");args.add(dayOfWeek);}
    sql.append(" order by te.day_of_week,te.start_time");
    return db.queryForList(sql.toString(),args.toArray());
  }

  @GetMapping("/resources")
  @PreAuthorize("hasAuthority('timetable.manage') or hasAuthority('dashboard.read')")
  public Map<String,Object> resources(){
    UUID t=TenantContextHolder.getRequired();
    return Map.of(
      "batches",db.queryForList("select id,name from batches where tenant_id=? and status='ACTIVE' order by name",t),
      "subjects",db.queryForList("select id,name from subjects where tenant_id=? and active=true order by name",t),
      "teachers",db.queryForList("select id,concat_ws(' ',first_name,last_name) name from teachers where tenant_id=? and status='ACTIVE' order by first_name,last_name",t),
      "classrooms",db.queryForList("select id,name from classrooms where tenant_id=? and active=true order by name",t)
    );
  }

  @GetMapping("/week")
  @PreAuthorize("hasAuthority('timetable.manage') or hasAuthority('dashboard.read')")
  public List<Map<String,Object>> week(@RequestParam UUID batchId){
    UUID t=TenantContextHolder.getRequired(); requireBatch(t,batchId);
    return list(batchId,null);
  }

  @PostMapping
  @PreAuthorize("hasAuthority('timetable.manage')")
  public Map<String,Object> create(@RequestBody EntryRequest r){
    UUID t=TenantContextHolder.getRequired();
    validate(r);
    requireBatch(t,r.batchId());
    validateReferences(t,r);
    ensureNoConflict(t,r,null);
    UUID id=UUID.randomUUID();
    db.update("""
      insert into timetable_entries
      (id,tenant_id,batch_id,subject_id,teacher_id,classroom_id,day_of_week,start_time,end_time)
      values(?,?,?,?,?,?,?,?,?)
      """,id,t,r.batchId(),r.subjectId(),r.teacherId(),r.classroomId(),r.dayOfWeek(),r.startTime(),r.endTime());
    return get(id,t);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('timetable.manage')")
  public Map<String,Object> update(@PathVariable UUID id,@RequestBody EntryRequest r){
    UUID t=TenantContextHolder.getRequired();
    if(!exists(id,t)) throw new IllegalArgumentException("Timetable entry not found");
    validate(r); requireBatch(t,r.batchId()); validateReferences(t,r); ensureNoConflict(t,r,id);
    db.update("""
      update timetable_entries set batch_id=?,subject_id=?,teacher_id=?,classroom_id=?,
      day_of_week=?,start_time=?,end_time=? where id=? and tenant_id=?
      """,r.batchId(),r.subjectId(),r.teacherId(),r.classroomId(),r.dayOfWeek(),r.startTime(),r.endTime(),id,t);
    return get(id,t);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('timetable.manage')")
  public Map<String,Object> delete(@PathVariable UUID id){
    UUID t=TenantContextHolder.getRequired();
    if(!exists(id,t)) throw new IllegalArgumentException("Timetable entry not found");
    db.update("delete from timetable_entries where id=? and tenant_id=?",id,t);
    return Map.of("deleted",true);
  }

  private void validate(EntryRequest r){
    if(r==null||r.batchId()==null||r.dayOfWeek()==null||r.startTime()==null||r.endTime()==null)
      throw new IllegalArgumentException("Batch, day, start time and end time are required");
    validateDay(r.dayOfWeek());
    if(!r.endTime().isAfter(r.startTime())) throw new IllegalArgumentException("End time must be after start time");
    if(r.subjectId()==null && r.teacherId()==null && r.classroomId()==null)
      throw new IllegalArgumentException("At least one of subject, teacher or classroom is required");
  }

  private void validateReferences(UUID t,EntryRequest r){
    if(r.subjectId()!=null && count("select count(*) from subjects where id=? and tenant_id=? and active=true",r.subjectId(),t)==0)
      throw new IllegalArgumentException("Subject is invalid for this institute");
    if(r.teacherId()!=null && count("select count(*) from teachers where id=? and tenant_id=? and status='ACTIVE'",r.teacherId(),t)==0)
      throw new IllegalArgumentException("Teacher is invalid for this institute");
    if(r.classroomId()!=null && count("select count(*) from classrooms where id=? and tenant_id=? and active=true",r.classroomId(),t)==0)
      throw new IllegalArgumentException("Classroom is invalid for this institute");
    if(r.teacherId()!=null && r.subjectId()!=null &&
       count("select count(*) from teacher_subjects ts join teachers th on th.id=ts.teacher_id where ts.teacher_id=? and ts.subject_id=? and th.tenant_id=?",r.teacherId(),r.subjectId(),t)==0)
      throw new IllegalArgumentException("Teacher is not assigned to this subject");
  }

  private void ensureNoConflict(UUID t,EntryRequest r,UUID currentId){
    String exclusion=currentId==null?"":" and te.id<>?";
    List<Object> argsBase=new ArrayList<>(List.of(t,r.dayOfWeek(),r.startTime(),r.endTime()));
    String time="te.start_time < ? and te.end_time > ?";
    if(r.teacherId()!=null){
      List<Object> a=new ArrayList<>(List.of(t,r.teacherId(),r.dayOfWeek(),r.endTime(),r.startTime()));
      if(currentId!=null)a.add(currentId);
      if(count("select count(*) from timetable_entries te where te.tenant_id=? and te.teacher_id=? and te.day_of_week=? and "+time.replace("te.start_time < ? and te.end_time > ?","te.start_time < ? and te.end_time > ?")+exclusion,a.toArray())>0)
        throw new IllegalArgumentException("Teacher has a timetable conflict");
    }
    if(r.batchId()!=null){
      List<Object> a=new ArrayList<>(List.of(t,r.batchId(),r.dayOfWeek(),r.endTime(),r.startTime()));
      if(currentId!=null)a.add(currentId);
      if(count("select count(*) from timetable_entries te where te.tenant_id=? and te.batch_id=? and te.day_of_week=? and te.start_time < ? and te.end_time > ?"+exclusion,a.toArray())>0)
        throw new IllegalArgumentException("Batch has a timetable conflict");
    }
    if(r.classroomId()!=null){
      List<Object> a=new ArrayList<>(List.of(t,r.classroomId(),r.dayOfWeek(),r.endTime(),r.startTime()));
      if(currentId!=null)a.add(currentId);
      if(count("select count(*) from timetable_entries te where te.tenant_id=? and te.classroom_id=? and te.day_of_week=? and te.start_time < ? and te.end_time > ?"+exclusion,a.toArray())>0)
        throw new IllegalArgumentException("Classroom has a timetable conflict");
    }
  }

  private void requireBatch(UUID t,UUID id){
    if(count("select count(*) from batches where id=? and tenant_id=? and status='ACTIVE'",id,t)==0)
      throw new IllegalArgumentException("Batch is invalid for this institute");
  }
  private void validateDay(int d){if(d<1||d>7)throw new IllegalArgumentException("dayOfWeek must be between 1 and 7");}
  private int count(String sql,Object...args){Integer n=db.queryForObject(sql,Integer.class,args);return n==null?0:n;}
  private boolean exists(UUID id,UUID t){return count("select count(*) from timetable_entries where id=? and tenant_id=?",id,t)>0;}
  private Map<String,Object> get(UUID id,UUID t){
    return db.queryForMap("""
      select te.id,te.batch_id,te.subject_id,te.teacher_id,te.classroom_id,
      te.day_of_week,te.start_time,te.end_time,b.name batch_name,
      coalesce(s.name,'General') subject_name,concat_ws(' ',th.first_name,th.last_name) teacher_name,
      c.name classroom_name from timetable_entries te
      join batches b on b.id=te.batch_id and b.tenant_id=te.tenant_id
      left join subjects s on s.id=te.subject_id and s.tenant_id=te.tenant_id
      left join teachers th on th.id=te.teacher_id and th.tenant_id=te.tenant_id
      left join classrooms c on c.id=te.classroom_id and c.tenant_id=te.tenant_id
      where te.id=? and te.tenant_id=?
      """,id,t);
  }

  public record EntryRequest(UUID batchId,UUID subjectId,UUID teacherId,UUID classroomId,
                             Integer dayOfWeek,LocalTime startTime,LocalTime endTime){}
}
