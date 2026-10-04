package com.projexa.coaching.portal.controller;

import com.projexa.coaching.common.tenant.TenantContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import com.projexa.coaching.common.security.ResourceAccess;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/portal")
public class PortalController {
 private final JdbcTemplate db; private final ResourceAccess access;
 public PortalController(JdbcTemplate db,ResourceAccess access){this.db=db;this.access=access;}

 @GetMapping("/me")
 public Map<String,Object> me(Authentication auth){
   UUID t=TenantContextHolder.getRequired(),u=UUID.fromString(auth.getName());
   List<Map<String,Object>> children=db.queryForList("""
     select s.id,s.admission_number,s.first_name,s.last_name,s.status,
            e.academic_year_id,e.class_id,e.stream_id,e.batch_id,
            c.name class_name,b.name batch_name,ay.name academic_year_name
     from students s join student_parents sp on sp.student_id=s.id
     join parents p on p.id=sp.parent_id
     left join enrollments e on e.student_id=s.id and e.tenant_id=s.tenant_id and e.status='ACTIVE'
     left join classes c on c.id=e.class_id left join batches b on b.id=e.batch_id left join academic_years ay on ay.id=e.academic_year_id
     where s.tenant_id=? and p.user_id=? order by s.first_name
     """,t,u);
   return Map.of("children",children,"role",auth.getAuthorities().stream().map(a->a.getAuthority()).filter(a->a.startsWith("ROLE_")).findFirst().orElse("ROLE_USER"));
 }

 @GetMapping("/student/{studentId}")
 @PreAuthorize("hasAnyRole('STUDENT','PARENT','TEACHER','INSTITUTE_OWNER','INSTITUTE_ADMIN')")
 public Map<String,Object> student(@PathVariable UUID studentId,Authentication auth){
   UUID t=TenantContextHolder.getRequired(); access.requireOwnStudent(t,studentId,auth);
   Map<String,Object> s=db.queryForMap("select id,admission_number,first_name,last_name,status,email,phone from students where id=? and tenant_id=?",studentId,t);
   List<Map<String,Object>> attendance=db.queryForList("select ar.status,count(*) total from attendance_records ar where ar.student_id=? and ar.tenant_id=? group by ar.status",studentId,t);
   List<Map<String,Object>> fees=db.queryForList("select invoice_number,amount,paid_amount,due_date,status from invoices where student_id=? and tenant_id=? order by due_date",studentId,t);
   List<Map<String,Object>> results=db.queryForList("select r.id,r.exam_id,r.total_marks,r.obtained_marks,r.percentage,r.rank,e.name exam_name,r.published_at from results r join exams e on e.id=r.exam_id where r.student_id=? and r.tenant_id=? and r.status='PUBLISHED' order by r.published_at desc",studentId,t);
   List<Map<String,Object>> homework=db.queryForList("select a.id,a.title,a.description,a.due_at,a.created_at,t.first_name teacher_first_name,t.last_name teacher_last_name from assignments a left join teachers t on t.id=a.teacher_id where a.tenant_id=? and a.batch_id=(select batch_id from enrollments where student_id=? and tenant_id=? and status='ACTIVE' order by enrolled_at desc limit 1) order by a.due_at desc nulls last limit 50",t,studentId,t);
   return Map.of("student",s,"attendance",attendance,"fees",fees,"results",results,"homework",homework);
 }
}