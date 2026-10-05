package com.projexa.coaching.portal.controller;

import com.projexa.coaching.common.tenant.TenantContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import com.projexa.coaching.common.security.ResourceAccess;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/portal")
public class PortalController {
 private final JdbcTemplate db;
 private final ResourceAccess access;
 public PortalController(JdbcTemplate db,ResourceAccess access){this.db=db;this.access=access;}

 @GetMapping("/me")
 public Map<String,Object> me(Authentication auth){
   UUID t=TenantContextHolder.getRequired(),u=UUID.fromString(auth.getName());
   List<Map<String,Object>> children=db.queryForList("""
     select distinct s.id,s.admission_number,s.first_name,s.last_name,s.status,
            e.academic_year_id,e.class_id,e.stream_id,e.batch_id,
            c.name class_name,b.name batch_name,ay.name academic_year_name
     from students s
     left join student_parents sp on sp.student_id=s.id
     left join parents p on p.id=sp.parent_id
     left join enrollments e on e.student_id=s.id and e.tenant_id=s.tenant_id and e.status='ACTIVE'
     left join classes c on c.id=e.class_id
     left join batches b on b.id=e.batch_id
     left join academic_years ay on ay.id=e.academic_year_id
     where s.tenant_id=? and (s.user_id=? or p.user_id=?)
     order by s.first_name
     """,t,u,u);
   String role=auth.getAuthorities().stream().map(a->a.getAuthority()).filter(a->a.startsWith("ROLE_")).findFirst().orElse("ROLE_USER");
   return Map.of("children",children,"role",role);
 }

 @GetMapping("/student/{studentId}")
 @PreAuthorize("hasAnyRole('STUDENT','PARENT','TEACHER','INSTITUTE_OWNER','INSTITUTE_ADMIN')")
 public Map<String,Object> student(@PathVariable UUID studentId,Authentication auth){
   UUID t=TenantContextHolder.getRequired(); access.requireOwnStudent(t,studentId,auth);
   Map<String,Object> s=db.queryForMap("select id,admission_number,first_name,last_name,status,email,phone from students where id=? and tenant_id=?",studentId,t);
   Map<String,Object> enrollment=singleOrEmpty("""
     select e.academic_year_id,e.class_id,e.stream_id,e.batch_id,c.name class_name,st.name stream_name,b.name batch_name,ay.name academic_year_name
     from enrollments e join classes c on c.id=e.class_id
     left join streams st on st.id=e.stream_id
     join batches b on b.id=e.batch_id join academic_years ay on ay.id=e.academic_year_id
     where e.student_id=? and e.tenant_id=? and e.status='ACTIVE'
     order by e.enrolled_at desc limit 1
     """,studentId,t);
   List<Map<String,Object>> attendance=db.queryForList("""
     select ar.status,count(*) total
     from attendance_records ar where ar.student_id=? and ar.tenant_id=? group by ar.status order by ar.status
     """,studentId,t);
   List<Map<String,Object>> attendanceRecent=db.queryForList("""
     select ats.session_date,ats.start_time,ats.end_time,coalesce(sub.name,'General') subject_name,ar.status
     from attendance_records ar join attendance_sessions ats on ats.id=ar.session_id
     left join subjects sub on sub.id=ats.subject_id and sub.tenant_id=ats.tenant_id
     where ar.student_id=? and ar.tenant_id=? order by ats.session_date desc,ats.start_time desc nulls last limit 30
     """,studentId,t);
   List<Map<String,Object>> fees=db.queryForList("""
     select invoice_number,amount,paid_amount,greatest(amount-paid_amount,0) balance,due_date,status
     from invoices where student_id=? and tenant_id=? order by due_date
     """,studentId,t);
   List<Map<String,Object>> results=db.queryForList("""
     select r.id,r.exam_id,r.total_marks,r.obtained_marks,r.percentage,r.rank,e.name exam_name,r.published_at
     from results r join exams e on e.id=r.exam_id
     where r.student_id=? and r.tenant_id=? and r.status='PUBLISHED'
     order by r.published_at desc
     """,studentId,t);
   List<Map<String,Object>> homework=db.queryForList("""
     select a.id,a.title,a.description,a.due_at,a.created_at,
            t.first_name teacher_first_name,t.last_name teacher_last_name,
            coalesce(su.status,'PENDING') submission_status,su.submitted_at,su.score,su.feedback
     from assignments a
     left join teachers t on t.id=a.teacher_id
     left join assignment_submissions su on su.assignment_id=a.id and su.student_id=?
     where a.tenant_id=? and a.batch_id=(select batch_id from enrollments where student_id=? and tenant_id=? and status='ACTIVE' order by enrolled_at desc limit 1)
     order by a.due_at desc nulls last limit 50
     """,studentId,t,studentId,t);
   List<Map<String,Object>> timetable=List.of();
   Object batchId=enrollment.get("batch_id");
   if(batchId!=null) timetable=db.queryForList("""
     select te.day_of_week,te.start_time,te.end_time,coalesce(su.name,'General') subject_name,
            coalesce(te.classroom_id::text,'') classroom_id,
            coalesce(t.first_name||' '||coalesce(t.last_name,''),'') teacher_name
     from timetable_entries te
     left join subjects su on su.id=te.subject_id and su.tenant_id=te.tenant_id
     left join teachers t on t.id=te.teacher_id and t.tenant_id=te.tenant_id
     where te.tenant_id=? and te.batch_id=? order by te.day_of_week,te.start_time
     """,t,batchId);
   List<Map<String,Object>> exams=db.queryForList("""
     select e.id,e.name,e.exam_type,e.starts_at,e.ends_at,e.status
     from exams e where e.tenant_id=? and e.academic_year_id=?
       and e.status in ('SCHEDULED','LIVE')
     order by e.starts_at nulls last limit 20
     """,t,enrollment.get("academic_year_id"));
   UUID user=UUID.fromString(auth.getName());
   List<Map<String,Object>> notifications=db.queryForList("""
     select id,type,title,body,read_at,created_at from notifications
     where tenant_id=? and user_id=? order by created_at desc limit 30
     """,t,user);
   return Map.of("student",s,"enrollment",enrollment,"attendance",attendance,"attendanceRecent",attendanceRecent,
     "fees",fees,"results",results,"homework",homework,"timetable",timetable,"exams",exams,"notifications",notifications);
 }

 @PostMapping("/student/{studentId}/homework/{assignmentId}/submit")
 @Transactional
 @PreAuthorize("hasRole('STUDENT')")
 public Map<String,Object> submitHomework(@PathVariable UUID studentId,@PathVariable UUID assignmentId,Authentication auth){
   UUID t=TenantContextHolder.getRequired(); access.requireOwnStudent(t,studentId,auth);
   Map<String,Object> assignment;
   try {
     assignment=db.queryForMap("""
       select a.id,a.batch_id,a.due_at from assignments a where a.id=? and a.tenant_id=?
         and a.batch_id=(select batch_id from enrollments where student_id=? and tenant_id=? and status='ACTIVE' order by enrolled_at desc limit 1)
       """,assignmentId,t,studentId,t);
   } catch(Exception e){throw new IllegalArgumentException("Homework is not assigned to this student");}
   db.update("""
     insert into assignment_submissions(id,tenant_id,assignment_id,student_id,submitted_at,status)
     values(?,?,?,?,CURRENT_TIMESTAMP,'SUBMITTED')
     on conflict(assignment_id,student_id) do update set submitted_at=excluded.submitted_at,status='SUBMITTED'
     """,UUID.randomUUID(),t,assignmentId,studentId);
   return Map.of("submitted",true,"assignmentId",assignmentId);
 }

 private Map<String,Object> singleOrEmpty(String sql,Object... args){
   List<Map<String,Object>> rows=db.queryForList(sql,args);
   return rows.isEmpty()?Map.of():rows.get(0);
 }
}