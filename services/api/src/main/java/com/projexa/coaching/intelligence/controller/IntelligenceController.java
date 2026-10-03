package com.projexa.coaching.intelligence.controller;

import com.projexa.coaching.common.tenant.TenantContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/intelligence")
public class IntelligenceController {
  private final JdbcTemplate db;
  public IntelligenceController(JdbcTemplate db){this.db=db;}

  @GetMapping("/overview")
  @PreAuthorize("hasAuthority('dashboard.read') or hasAuthority('reports.read')")
  public Map<String,Object> overview(){
    UUID t=TenantContextHolder.getRequired();
    Map<String,Object> attendance=db.queryForMap("""
      select coalesce(round(100.0*count(*) filter(where ar.status in ('PRESENT','LATE'))/nullif(count(ar.id),0),2),0) attendance_percentage,
             count(distinct e.student_id) students
      from enrollments e left join attendance_sessions s on s.tenant_id=e.tenant_id and s.batch_id=e.batch_id and s.session_date>=current_date-30
      left join attendance_records ar on ar.session_id=s.id and ar.student_id=e.student_id and ar.tenant_id=e.tenant_id
      where e.tenant_id=? and e.status='ACTIVE'
      """,t);
    Map<String,Object> fees=db.queryForMap("""
      select count(*) overdue_invoices,coalesce(sum(greatest(amount-paid_amount,0)),0) overdue_amount
      from invoices where tenant_id=? and status in ('UNPAID','PARTIALLY_PAID') and due_date<current_date
      """,t);
    Map<String,Object> exams=db.queryForMap("""
      select count(distinct exam_id) exams,count(*) published_results,coalesce(avg(percentage),0) average_percentage
      from results where tenant_id=? and status='PUBLISHED' and published_at>=current_date-90
      """,t);
    return Map.of("attendance",attendance,"fees",fees,"exams",exams);
  }

  @GetMapping("/risk-students")
  @PreAuthorize("hasAuthority('dashboard.read') or hasAuthority('reports.read')")
  public List<Map<String,Object>> riskStudents(){
    UUID t=TenantContextHolder.getRequired();
    return db.queryForList("""
      with att as (
        select e.student_id,coalesce(round(100.0*count(*) filter(where ar.status in ('PRESENT','LATE'))/nullif(count(ar.id),0),2),100) attendance_percentage
        from enrollments e left join attendance_sessions s on s.tenant_id=e.tenant_id and s.batch_id=e.batch_id and s.session_date>=current_date-30
        left join attendance_records ar on ar.session_id=s.id and ar.student_id=e.student_id and ar.tenant_id=e.tenant_id
        where e.tenant_id=? and e.status='ACTIVE' group by e.student_id
      ), fees as (
        select student_id,count(*) overdue_count,coalesce(sum(greatest(amount-paid_amount,0)),0) overdue_amount
        from invoices where tenant_id=? and status in ('UNPAID','PARTIALLY_PAID') and due_date<current_date group by student_id
      ), perf as (
        select student_id,avg(percentage) average_percentage,max(published_at) latest_result
        from results where tenant_id=? and status='PUBLISHED' group by student_id
      )
      select st.id student_id,st.admission_number,st.first_name,st.last_name,
             coalesce(att.attendance_percentage,100) attendance_percentage,
             coalesce(fees.overdue_count,0) overdue_count,coalesce(fees.overdue_amount,0) overdue_amount,
             coalesce(perf.average_percentage,0) average_percentage,
             (case when coalesce(att.attendance_percentage,100)<60 then 3 else 0 end+
              case when coalesce(fees.overdue_count,0)>0 then 2 else 0 end+
              case when coalesce(perf.average_percentage,100)<50 then 3 else 0 end) risk_score
      from students st left join att on att.student_id=st.id left join fees on fees.student_id=st.id left join perf on perf.student_id=st.id
      where st.tenant_id=? and st.status='ACTIVE'
      order by risk_score desc,attendance_percentage,average_percentage
      limit 50
      """,t,t,t,t);
  }

  @GetMapping("/exams/{examId}/topics")
  @PreAuthorize("hasAuthority('results.read') or hasAuthority('results.manage') or hasAuthority('reports.read')")
  public List<Map<String,Object>> topics(@PathVariable UUID examId){
    UUID t=TenantContextHolder.getRequired();
    requireExam(t,examId);
    return db.queryForList("""
      select coalesce(q.topic,'Uncategorized') topic,q.subject_id,s.name subject_name,
             count(*) question_attempts,
             count(*) filter(where aa.is_correct=true) correct_attempts,
             coalesce(round(100.0*count(*) filter(where aa.is_correct=true)/nullif(count(*),0),2),0) accuracy,
             coalesce(avg(aa.marks_awarded),0) average_marks
      from attempt_answers aa
      join exam_attempts ea on ea.id=aa.attempt_id and ea.tenant_id=? and ea.exam_id=? and ea.status='SUBMITTED'
      join questions q on q.id=aa.question_id and q.tenant_id=?
      left join subjects s on s.id=q.subject_id and s.tenant_id=q.tenant_id
      group by coalesce(q.topic,'Uncategorized'),q.subject_id,s.name
      order by accuracy asc,question_attempts desc
      """,t,examId,t);
  }

  @GetMapping("/exams/{examId}/recommendations")
  @PreAuthorize("hasAuthority('results.read') or hasAuthority('results.manage') or hasAuthority('reports.read')")
  public List<Map<String,Object>> recommendations(@PathVariable UUID examId){
    UUID t=TenantContextHolder.getRequired(); requireExam(t,examId);
    List<Map<String,Object>> topics=topics(examId);
    List<Map<String,Object>> out=new ArrayList<>();
    for(Map<String,Object> row:topics){
      double accuracy=((Number)row.get("accuracy")).doubleValue();
      if(accuracy<50) out.add(Map.of("priority",accuracy<30?"HIGH":"MEDIUM","topic",String.valueOf(row.get("topic")),"subject",String.valueOf(row.get("subject_name")),"action","Schedule targeted revision and assign additional practice questions"));
    }
    return out;
  }

  @PostMapping("/students/{studentId}/watchlist")
  @PreAuthorize("hasAuthority('dashboard.read') or hasAuthority('students.manage')")
  public Map<String,Object> watchlist(@PathVariable UUID studentId,@RequestBody Map<String,String> req){
    UUID t=TenantContextHolder.getRequired();
    if(count("select count(*) from students where id=? and tenant_id=? and status='ACTIVE'",studentId,t)==0) throw new IllegalArgumentException("Student not found");
    String reason=req.getOrDefault("reason","AI risk signal");
    db.update("insert into student_watchlist(id,tenant_id,student_id,reason,active) values(?,?,?,?,true) on conflict(tenant_id,student_id) do update set reason=excluded.reason,active=true",
      UUID.randomUUID(),t,studentId,reason);
    return Map.of("watchlisted",true,"studentId",studentId);
  }

  @PostMapping("/students/{studentId}/task")
  @PreAuthorize("hasAuthority('dashboard.read') or hasAuthority('students.manage')")
  public Map<String,Object> task(@PathVariable UUID studentId,@RequestBody Map<String,String> req){
    UUID t=TenantContextHolder.getRequired();
    if(count("select count(*) from students where id=? and tenant_id=? and status='ACTIVE'",studentId,t)==0) throw new IllegalArgumentException("Student not found");
    String title=req.getOrDefault("title","Follow up on AI risk signal");
    UUID assignedTo=req.get("assignedTo")==null||req.get("assignedTo").isBlank()?null:UUID.fromString(req.get("assignedTo"));
    UUID id=UUID.randomUUID();
    db.update("insert into staff_tasks(id,tenant_id,assigned_to,student_id,title,status) values(?,?,?,?,?,'OPEN')",id,t,assignedTo,studentId,title);
    return Map.of("taskId",id,"created",true);
  }

  @GetMapping("/tasks")
  @PreAuthorize("hasAuthority('dashboard.read') or hasAuthority('students.manage')")
  public List<Map<String,Object>> tasks(){
    UUID t=TenantContextHolder.getRequired();
    return db.queryForList("select id,assigned_to,student_id,title,due_at,status,created_at from staff_tasks where tenant_id=? and status<>'DONE' order by due_at nulls last,created_at desc limit 100",t);
  }

  private void requireExam(UUID t,UUID id){if(count("select count(*) from exams where id=? and tenant_id=?",id,t)==0)throw new IllegalArgumentException("Exam not found");}
  private int count(String sql,Object... args){Integer n=db.queryForObject(sql,Integer.class,args);return n==null?0:n;}
}