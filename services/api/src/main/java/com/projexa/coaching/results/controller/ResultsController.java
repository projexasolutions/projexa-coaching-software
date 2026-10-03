package com.projexa.coaching.results.controller;
import com.projexa.coaching.common.tenant.TenantContextHolder;
import com.projexa.coaching.common.security.ResourceAccess;
import org.springframework.security.core.Authentication;
import org.springframework.jdbc.core.JdbcTemplate; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*; import java.time.LocalDateTime; import java.util.*;
@RestController @RequestMapping("/api/v1/results")
public class ResultsController {
 private final JdbcTemplate db; private final ResourceAccess access; public ResultsController(JdbcTemplate db, ResourceAccess access){this.db=db;this.access=access;}
 @PostMapping("/exams/{examId}/generate") @PreAuthorize("hasAuthority('results.manage') or hasRole('INSTITUTE_OWNER')")
 public Map<String,Object> generate(@PathVariable UUID examId){UUID t=TenantContextHolder.getRequired();
   List<Map<String,Object>> attempts=db.queryForList("select student_id,score from exam_attempts where tenant_id=? and exam_id=? and status='SUBMITTED'",t,examId);
   double max=db.queryForObject("select coalesce(sum(max_marks),0) from exam_subjects where tenant_id=? and exam_id=?",Double.class,t,examId);
   attempts.sort((a,b)->Double.compare(((Number)b.get("score")).doubleValue(),((Number)a.get("score")).doubleValue())); int rank=0; double previous=-1;
   for(int i=0;i<attempts.size();i++){double score=((Number)attempts.get(i).get("score")).doubleValue(); if(score!=previous)rank=i+1; previous=score; UUID student=(UUID)attempts.get(i).get("student_id"); double pct=max==0?0:score*100/max;
     db.update("insert into results(id,tenant_id,exam_id,student_id,total_marks,obtained_marks,percentage,rank,status) values(?,?,?,?,?,?,?,?,?) on conflict(exam_id,student_id) do update set total_marks=excluded.total_marks,obtained_marks=excluded.obtained_marks,percentage=excluded.percentage,rank=excluded.rank,status='DRAFT'",UUID.randomUUID(),t,examId,student,max,score,pct,rank,"DRAFT"); }
   return Map.of("examId",examId,"generated",attempts.size(),"maxMarks",max);
 }
 @PostMapping("/exams/{examId}/publish") @PreAuthorize("hasAuthority('results.manage') or hasRole('INSTITUTE_OWNER')") public Map<String,Object> publish(@PathVariable UUID examId){UUID t=TenantContextHolder.getRequired();int n=db.update("update results set status='PUBLISHED',published_at=? where tenant_id=? and exam_id=?",LocalDateTime.now(),t,examId);return Map.of("published",n);}
 @GetMapping("/students/{studentId}") public List<Map<String,Object>> student(@PathVariable UUID studentId, Authentication auth){access.requireOwnStudent(TenantContextHolder.getRequired(), studentId, auth);return db.queryForList("select r.exam_id,r.obtained_marks,r.total_marks,r.percentage,r.rank,r.status,e.name exam_name from results r join exams e on e.id=r.exam_id where r.tenant_id=? and r.student_id=? and r.status='PUBLISHED' order by r.published_at desc",TenantContextHolder.getRequired(),studentId);}
}
