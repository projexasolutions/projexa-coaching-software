package com.projexa.coaching.attendance.controller;

import com.projexa.coaching.common.tenant.TenantContextHolder;
import com.projexa.coaching.common.security.ResourceAccess;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/attendance")
public class AttendanceController {
  private static final Set<String> STATUSES = Set.of("UNMARKED","PRESENT","ABSENT","LATE","LEAVE");
  private final JdbcTemplate db;
  private final ResourceAccess access;

  public AttendanceController(JdbcTemplate db, ResourceAccess access){this.db=db;this.access=access;}

  @GetMapping("/sessions")
  @PreAuthorize("hasAuthority('attendance.manage') or hasAuthority('dashboard.read')")
  public List<Map<String,Object>> sessions(@RequestParam UUID batchId,@RequestParam LocalDate date){
    UUID t=TenantContextHolder.getRequired();
    requireBatch(t,batchId);
    return db.queryForList("select s.id,s.batch_id,s.subject_id,s.teacher_id,s.session_date,s.start_time,s.end_time,s.status,b.name batch_name,coalesce(sub.name,'General') subject_name from attendance_sessions s join batches b on b.id=s.batch_id left join subjects sub on sub.id=s.subject_id where s.tenant_id=? and s.batch_id=? and s.session_date=? order by s.start_time nulls last",t,batchId,date);
  }

  @PostMapping("/sessions")
  @PreAuthorize("hasAuthority('attendance.manage')")
  public Map<String,Object> createSession(@RequestBody SessionRequest req){
    UUID t=TenantContextHolder.getRequired();
    validateSession(req);
    requireBatch(t,req.batchId());
    if(req.subjectId()!=null && count("select count(*) from subjects where id=? and tenant_id=? and active=true",req.subjectId(),t)==0) throw new IllegalArgumentException("Subject is invalid for this institute");
    if(req.teacherId()!=null && count("select count(*) from teachers where id=? and tenant_id=? and status='ACTIVE'",req.teacherId(),t)==0) throw new IllegalArgumentException("Teacher is invalid for this institute");
    if(req.subjectId()!=null && req.teacherId()!=null && count("select count(*) from teacher_subjects ts join teachers te on te.id=ts.teacher_id where ts.teacher_id=? and ts.subject_id=? and te.tenant_id=?",req.teacherId(),req.subjectId(),t)==0) throw new IllegalArgumentException("Teacher is not assigned to this subject");
    if(count("select count(*) from attendance_sessions where tenant_id=? and batch_id=? and session_date=? and coalesce(subject_id,'00000000-0000-0000-0000-000000000000')=coalesce(?, '00000000-0000-0000-0000-000000000000') and coalesce(start_time,'00:00:00')=coalesce(?,'00:00:00')",t,req.batchId(),req.sessionDate(),req.subjectId(),req.startTime())>0)
      throw new IllegalArgumentException("An attendance session already exists for this batch, date, subject and start time");
    UUID id=UUID.randomUUID();
    db.update("insert into attendance_sessions(id,tenant_id,batch_id,subject_id,teacher_id,session_date,start_time,end_time,status) values(?,?,?,?,?,?,?,?,?)",id,t,req.batchId(),req.subjectId(),req.teacherId(),req.sessionDate(),req.startTime(),req.endTime(),"OPEN");
    return session(id,t);
  }

  @GetMapping("/sessions/{sessionId}/records")
  @PreAuthorize("hasAuthority('attendance.manage') or hasAuthority('dashboard.read')")
  public List<Map<String,Object>> records(@PathVariable UUID sessionId){
    UUID t=TenantContextHolder.getRequired();
    UUID batchId=batchId(sessionId,t);
    return db.queryForList("select st.id student_id,st.admission_number,st.first_name,st.last_name,coalesce(ar.status,'UNMARKED') status,ar.marked_at from students st join enrollments e on e.student_id=st.id and e.tenant_id=st.tenant_id and e.status='ACTIVE' left join attendance_records ar on ar.student_id=st.id and ar.session_id=? where st.tenant_id=? and e.batch_id=? order by st.first_name,st.last_name",sessionId,t,batchId);
  }

  @GetMapping("/sessions/{sessionId}/summary")
  @PreAuthorize("hasAuthority('attendance.manage') or hasAuthority('dashboard.read')")
  public Map<String,Object> summary(@PathVariable UUID sessionId){
    UUID t=TenantContextHolder.getRequired();
    UUID batchId=batchId(sessionId,t);
    Map<String,Object> r=db.queryForMap("select count(*) total,count(*) filter(where coalesce(ar.status,'UNMARKED')='PRESENT') present,count(*) filter(where coalesce(ar.status,'UNMARKED')='ABSENT') absent,count(*) filter(where coalesce(ar.status,'UNMARKED')='LATE') late,count(*) filter(where coalesce(ar.status,'UNMARKED')='LEAVE') leave,count(*) filter(where coalesce(ar.status,'UNMARKED')='UNMARKED') unmarked from enrollments e join students st on st.id=e.student_id and st.tenant_id=e.tenant_id left join attendance_records ar on ar.session_id=? and ar.student_id=st.id and ar.tenant_id=? where e.tenant_id=? and e.batch_id=? and e.status='ACTIVE'",sessionId,t,t,batchId);
    return r;
  }

  @GetMapping("/batch/{batchId}/summary")
  @PreAuthorize("hasAuthority('attendance.manage') or hasAuthority('dashboard.read')")
  public Map<String,Object> batchSummary(@PathVariable UUID batchId,@RequestParam(defaultValue="30") int days){
    UUID t=TenantContextHolder.getRequired(); requireBatch(t,batchId);
    int safeDays=Math.max(1,Math.min(days,366));
    return db.queryForMap("select count(distinct s.id) sessions,count(distinct e.student_id) students,count(ar.id) marked,count(ar.id) filter(where ar.status='PRESENT') present,count(ar.id) filter(where ar.status='ABSENT') absent,count(ar.id) filter(where ar.status='LATE') late,count(ar.id) filter(where ar.status='LEAVE') leave,coalesce(round(100.0*count(ar.id) filter(where ar.status in ('PRESENT','LATE'))/nullif(count(ar.id),0),2),0) attendance_percentage from attendance_sessions s join enrollments e on e.tenant_id=s.tenant_id and e.batch_id=s.batch_id and e.status='ACTIVE' left join attendance_records ar on ar.tenant_id=s.tenant_id and ar.session_id=s.id and ar.student_id=e.student_id where s.tenant_id=? and s.batch_id=? and s.session_date>=current_date-?::int group by s.batch_id",t,batchId,safeDays);
  }

  @PostMapping("/sessions/{sessionId}/records/bulk")
  @PreAuthorize("hasAuthority('attendance.manage')")
  public Map<String,Object> bulk(@PathVariable UUID sessionId,@RequestBody BulkRequest req){
    UUID t=TenantContextHolder.getRequired(); UUID batchId=batchId(sessionId,t);
    if(req==null || req.records()==null) throw new IllegalArgumentException("Attendance records are required");
    for(RecordItem item:req.records()){
      if(item==null || item.studentId()==null) throw new IllegalArgumentException("Student id is required");
      validateStatus(item.status());
      if(count("select count(*) from enrollments where tenant_id=? and student_id=? and batch_id=? and status='ACTIVE'",t,item.studentId(),batchId)==0)
        throw new IllegalArgumentException("Student is not actively enrolled in this batch: "+item.studentId());
      db.update("insert into attendance_records(id,tenant_id,session_id,student_id,status,marked_at) values(?,?,?,?,?,now()) on conflict(session_id,student_id) do update set tenant_id=excluded.tenant_id,status=excluded.status,marked_at=now()",UUID.randomUUID(),t,sessionId,item.studentId(),item.status());
    }
    return Map.of("updated",req.records().size());
  }

  @PostMapping("/sessions/{sessionId}/check-in")
  @PreAuthorize("hasRole('STUDENT') or hasRole('INSTITUTE_OWNER') or hasRole('INSTITUTE_ADMIN')")
  public Map<String,Object> checkIn(@PathVariable UUID sessionId,@RequestBody CheckIn req,Authentication auth){
    UUID t=TenantContextHolder.getRequired(); UUID batchId=batchId(sessionId,t);
    access.requireOwnStudent(t,req.studentId(),auth);
    if(count("select count(*) from enrollments where tenant_id=? and student_id=? and batch_id=? and status='ACTIVE'",t,req.studentId(),batchId)==0) throw new IllegalArgumentException("Student is not enrolled in this batch");
    if(isRestricted(req.studentId(),t)) return Map.of("allowed",false,"code","FEE_OVERDUE","message","Attendance self check-in is restricted until outstanding fees are cleared");
    db.update("insert into attendance_records(id,tenant_id,session_id,student_id,status,marked_at) values(?,?,?,?,?,now()) on conflict(session_id,student_id) do update set tenant_id=excluded.tenant_id,status=excluded.status,marked_at=now()",UUID.randomUUID(),t,req.studentId(),"PRESENT");
    return Map.of("allowed",true,"status","PRESENT");
  }

  private void validateSession(SessionRequest r){
    if(r==null || r.batchId()==null || r.sessionDate()==null) throw new IllegalArgumentException("Batch and session date are required");
    if(r.startTime()!=null && r.endTime()!=null && !r.endTime().isAfter(r.startTime())) throw new IllegalArgumentException("End time must be after start time");
  }
  private void validateStatus(String s){if(s==null || !STATUSES.contains(s.trim().toUpperCase())) throw new IllegalArgumentException("Invalid attendance status");}
  private void requireBatch(UUID t,UUID batchId){if(batchId==null || count("select count(*) from batches where id=? and tenant_id=? and status='ACTIVE'",batchId,t)==0) throw new IllegalArgumentException("Batch is invalid for this institute");}
  private int count(String sql,Object... args){Integer n=db.queryForObject(sql,Integer.class,args);return n==null?0:n;}
  private UUID batchId(UUID sessionId,UUID t){try{return db.queryForObject("select batch_id from attendance_sessions where id=? and tenant_id=?",UUID.class,sessionId,t);}catch(Exception e){throw new IllegalArgumentException("Attendance session not found");}}
  private Map<String,Object> session(UUID id,UUID t){return db.queryForMap("select id,batch_id,subject_id,teacher_id,session_date,start_time,end_time,status from attendance_sessions where id=? and tenant_id=?",id,t);}
  private boolean isRestricted(UUID student,UUID t){
    try{
      Map<String,Object> s=db.queryForMap("select settings from tenant_settings where tenant_id=?",t);
      String json=String.valueOf(s.get("settings"));
      if(!json.contains(""feeAttendanceRestriction"")||!json.contains(""enabled":true")) return false;
      Integer manual=db.queryForObject("select count(*) from student_attendance_restrictions where tenant_id=? and student_id=? and active=true",Integer.class,t,student);
      if(manual!=null&&manual>0)return true;
      Integer overdue=db.queryForObject("select count(*) from invoices where tenant_id=? and student_id=? and status in ('UNPAID','PARTIALLY_PAID') and due_date<current_date",Integer.class,t,student);
      return overdue!=null&&overdue>0;
    }catch(Exception e){throw new IllegalStateException("Unable to verify attendance restriction policy",e);}
  }
  public record SessionRequest(UUID batchId,UUID subjectId,UUID teacherId,LocalDate sessionDate,LocalTime startTime,LocalTime endTime){}
  public record RecordItem(UUID studentId,String status){}
  public record BulkRequest(List<RecordItem> records){}
  public record CheckIn(UUID studentId){}
}
