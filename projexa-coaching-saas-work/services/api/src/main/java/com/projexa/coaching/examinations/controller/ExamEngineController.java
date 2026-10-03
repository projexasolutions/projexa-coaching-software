package com.projexa.coaching.examinations.controller;

import com.projexa.coaching.common.tenant.TenantContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.security.core.Authentication; import com.projexa.coaching.common.security.ResourceAccess;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/exams")
public class ExamEngineController {
  private final JdbcTemplate db; private final ResourceAccess access;
  public ExamEngineController(JdbcTemplate db, ResourceAccess access){this.db=db; this.access=access;}

  @PostMapping("/{examId}/attempts")
  @org.springframework.transaction.annotation.Transactional
  @PreAuthorize("hasAuthority('exams.manage') or hasRole('STUDENT')")
  public ResponseEntity<?> start(@PathVariable UUID examId,@RequestBody StartAttempt req, Authentication auth){
    UUID tenant=TenantContextHolder.getRequired(); access.requireOwnStudent(tenant, req.studentId(), auth);
    UUID existing=findAttempt(examId,req.studentId(),tenant);
    if(existing!=null) return ResponseEntity.ok(Map.of("attemptId",existing,"status","IN_PROGRESS"));
    UUID id=UUID.randomUUID();
    db.update("insert into exam_attempts(id,tenant_id,exam_id,student_id,started_at,status) values(?,?,?,?,?,?)",id,tenant,examId,req.studentId(),LocalDateTime.now(),"IN_PROGRESS");
    return ResponseEntity.ok(Map.of("attemptId",id,"status","IN_PROGRESS"));
  }

  @PostMapping("/attempts/{attemptId}/submit")
  @org.springframework.transaction.annotation.Transactional
  @PreAuthorize("hasAuthority('exams.manage') or hasRole('STUDENT')")
  public ResponseEntity<?> submit(@PathVariable UUID attemptId,@RequestBody SubmitAttempt req, Authentication auth){
    UUID tenant=TenantContextHolder.getRequired();
    Map<String,Object> attempt=db.queryForMap("select exam_id,student_id,status from exam_attempts where id=? and tenant_id=?",attemptId,tenant);
    if(!"IN_PROGRESS".equals(attempt.get("status"))) return ResponseEntity.badRequest().body(Map.of("code","ATTEMPT_CLOSED","message","Attempt is already submitted"));
    UUID studentId=(UUID)attempt.get("student_id"); access.requireOwnStudent(tenant, studentId, auth); UUID examId=(UUID)attempt.get("exam_id"); double total=0;
    for(var e:req.answers().entrySet()){
      UUID q=UUID.fromString(e.getKey()); String answer=String.valueOf(e.getValue());
      Map<String,Object> qrow=db.queryForMap("select q.marks,q.question_type,q.subject_id,es.negative_marking from questions q join exam_subjects es on es.exam_id=? and es.subject_id=q.subject_id where q.id=? and q.tenant_id=?",examId,q,tenant);
      List<Map<String,Object>> options=db.queryForList("select option_text,is_correct from question_options where question_id=? order by display_order",q);
      boolean correct=grade(String.valueOf(qrow.get("question_type")),answer,options);
      BigDecimal marks=(BigDecimal)qrow.get("marks"); BigDecimal configuredNegative=(BigDecimal)qrow.get("negative_marking"); double awarded=correct?marks.doubleValue():(configuredNegative != null && configuredNegative.signum()>0 ? -configuredNegative.doubleValue() : 0);
      db.update("insert into attempt_answers(id,attempt_id,question_id,answer_text,is_correct,marks_awarded) values(?,?,?,?,?,?) on conflict (attempt_id,question_id) do update set answer_text=excluded.answer_text,is_correct=excluded.is_correct,marks_awarded=excluded.marks_awarded",UUID.randomUUID(),attemptId,q,answer,correct,awarded);
      total+=awarded;
    }
    db.update("update exam_attempts set score=?,submitted_at=?,status='SUBMITTED' where id=? and tenant_id=?",total,LocalDateTime.now(),attemptId,tenant);
    return ResponseEntity.ok(Map.of("attemptId",attemptId,"score",total,"status","SUBMITTED"));
  }

  @GetMapping("/{examId}/analysis")
  @PreAuthorize("hasAnyRole('INSTITUTE_OWNER','INSTITUTE_ADMIN','TEACHER')")
  public Map<String,Object> analysis(@PathVariable UUID examId){
    UUID t=TenantContextHolder.getRequired();
    Map<String,Object> row=db.queryForMap("select count(*) attempts, coalesce(avg(score),0) average_score, coalesce(max(score),0) highest_score from exam_attempts where tenant_id=? and exam_id=? and status='SUBMITTED'",t,examId);
    return row;
  }
  private UUID findAttempt(UUID exam,UUID student,UUID tenant){List<UUID> x=db.query("select id from exam_attempts where exam_id=? and student_id=? and tenant_id=? and status='IN_PROGRESS'",(rs,n)->(UUID)rs.getObject(1),exam,student,tenant);return x.isEmpty()?null:x.get(0);}
  private boolean grade(String type,String answer,List<Map<String,Object>> options){
    String a=answer.trim();
    if(type.equals("MCQ_SINGLE")||type.equals("TRUE_FALSE")) return options.stream().anyMatch(o->Boolean.TRUE.equals(o.get("is_correct")) && a.equalsIgnoreCase(String.valueOf(o.get("option_text"))));
    if(type.equals("MCQ_MULTIPLE")||type.equals("MATCHING")){Set<String> given=new TreeSet<>(String.CASE_INSENSITIVE_ORDER);given.addAll(Arrays.asList(a.split(",")));Set<String> correct=new TreeSet<>(String.CASE_INSENSITIVE_ORDER);options.stream().filter(o->Boolean.TRUE.equals(o.get("is_correct"))).forEach(o->correct.add(String.valueOf(o.get("option_text"))));return given.equals(correct);}
    if(type.equals("FILL_BLANK")) return options.stream().filter(o->Boolean.TRUE.equals(o.get("is_correct"))).anyMatch(o->a.equalsIgnoreCase(String.valueOf(o.get("option_text"))));
    return false;
  }
  private double negative(double marks,boolean enabled){return enabled?-marks:0;}
  public record StartAttempt(UUID studentId){}
  public record SubmitAttempt(Map<String,Object> answers){}
}
