package com.projexa.coaching.attendance.controller;

import com.projexa.coaching.common.tenant.TenantContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import com.projexa.coaching.common.security.ResourceAccess;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/attendance")
public class AttendanceController {
  private final JdbcTemplate db; private final ResourceAccess access;
  public AttendanceController(JdbcTemplate db, ResourceAccess access){this.db=db;this.access=access;}

  @GetMapping("/sessions")
  @PreAuthorize("hasAuthority('attendance.manage') or hasAuthority('dashboard.read')")
  public List<Map<String,Object>> sessions(@RequestParam UUID batchId,@RequestParam LocalDate date){
    UUID t=TenantContextHolder.getRequired();
    return db.queryForList("select s.id,s.batch_id,s.subject_id,s.teacher_id,s.session_date,s.start_time,s.end_time,s.status,b.name batch_name,coalesce(sub.name,'General') subject_name from attendance_sessions s join batches b on b.id=s.batch_id left join subjects sub on sub.id=s.subject_id where s.tenant_id=? and s.batch_id=? and s.session_date=? order by s.start_time nulls last",t,batchId,date);
  }

  @PostMapping("/sessions")
  @PreAuthorize("hasAuthority('attendance.manage')")
  public Map<String,Object> createSession(@RequestBody SessionRequest req){
    UUID t=TenantContextHolder.getRequired(); UUID id=UUID.randomUUID();
    db.update("insert into attendance_sessions(id,tenant_id,batch_id,subject_id,teacher_id,session_date,start_time,end_time,status) values(?,?,?,?,?,?,?,?,?)",
      id,t,req.batchId(),req.subjectId(),req.teacherId(),req.sessionDate(),req.startTime(),req.endTime(),"OPEN");
    return session(id,t);
  }

  @GetMapping("/sessions/{sessionId}/records")
  @PreAuthorize("hasAuthority('attendance.manage') or hasAuthority('dashboard.read')")
  public List<Map<String,Object>> records(@PathVariable UUID sessionId){
    UUID t=TenantContextHolder.getRequired();
    return db.queryForList("select st.id student_id,st.admission_number,st.first_name,st.last_name,coalesce(ar.status,'UNMARKED') status,ar.marked_at from students st join enrollments e on e.student_id=st.id and e.tenant_id=st.tenant_id and e.status='ACTIVE' left join attendance_records ar on ar.student_id=st.id and ar.session_id=? where st.tenant_id=? and e.batch_id=? order by st.first_name,st.last_name",
      sessionId,t,batchId(sessionId,t));
  }

  @PostMapping("/sessions/{sessionId}/records/bulk")
  @PreAuthorize("hasAuthority('attendance.manage')")
  public Map<String,Object> bulk(@PathVariable UUID sessionId,@RequestBody BulkRequest req){
    UUID t=TenantContextHolder.getRequired();
    for(RecordItem item:req.records()){
      db.update("insert into attendance_records(id,tenant_id,session_id,student_id,status,marked_at) values(?,?,?,?,?,now()) on conflict(session_id,student_id) do update set status=excluded.status,marked_at=now()",UUID.randomUUID(),t,sessionId,item.studentId(),item.status());
    }
    return Map.of("updated",req.records().size());
  }

  @PostMapping("/sessions/{sessionId}/check-in")
  @PreAuthorize("hasRole('STUDENT') or hasRole('INSTITUTE_OWNER') or hasRole('INSTITUTE_ADMIN')")
  public Map<String,Object> checkIn(@PathVariable UUID sessionId,@RequestBody CheckIn req,Authentication auth){
    UUID t=TenantContextHolder.getRequired(); access.requireOwnStudent(t,req.studentId(),auth);
    if(isRestricted(req.studentId(),t)) return Map.of("allowed",false,"code","FEE_OVERDUE","message","Attendance self check-in is restricted until outstanding fees are cleared");
    db.update("insert into attendance_records(id,tenant_id,session_id,student_id,status) values(?,?,?,?,?) on conflict(session_id,student_id) do update set status=excluded.status,marked_at=now()",UUID.randomUUID(),t,sessionId,req.studentId(),"PRESENT");
    return Map.of("allowed",true,"status","PRESENT");
  }

  private UUID batchId(UUID sessionId,UUID t){return db.queryForObject("select batch_id from attendance_sessions where id=? and tenant_id=?",UUID.class,sessionId,t);}
  private Map<String,Object> session(UUID id,UUID t){return db.queryForMap("select id,batch_id,subject_id,teacher_id,session_date,start_time,end_time,status from attendance_sessions where id=? and tenant_id=?",id,t);}
  private boolean isRestricted(UUID student,UUID t){
    try{
      Map<String,Object> s=db.queryForMap("select settings from tenant_settings where tenant_id=?",t);
      String json=String.valueOf(s.get("settings"));
      if(!json.contains("feeAttendanceRestriction")||!json.contains("\"enabled\":true")) return false;
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
