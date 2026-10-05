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
    UUID tenant=TenantContextHolder.getRequired();
    if(req==null || req.studentId()==null) throw new IllegalArgumentException("Student is required");
    access.requireOwnStudent(tenant, req.studentId(), auth);
    Map<String,Object> exam=exam(examId,tenant);
    if(!Set.of("SCHEDULED","LIVE").contains(String.valueOf(exam.get("status")))) throw new IllegalArgumentException("Exam is not open for attempts");
    if(count("select count(*) from enrollments where tenant_id=? and student_id=? and status='ACTIVE'",tenant,req.studentId())==0) throw new IllegalArgumentException("Student is not actively enrolled");
    if(count("select count(*) from exam_attempts where tenant_id=? and exam_id=? and student_id=? and status='SUBMITTED'",tenant,examId,req.studentId())>0) throw new IllegalArgumentException("Student has already submitted this exam");
    LocalDateTime now=LocalDateTime.now();
    if(exam.get("starts_at")!=null && now.isBefore(timestamp(exam.get("starts_at")))) throw new IllegalArgumentException("Exam has not started");
    if(exam.get("ends_at")!=null && now.isAfter(timestamp(exam.get("ends_at")))) throw new IllegalArgumentException("Exam has ended");
    UUID existing=findAttemptForUpdate(examId,req.studentId(),tenant);
    if(existing!=null) return ResponseEntity.ok(Map.of("attemptId",existing,"status","IN_PROGRESS"));
    UUID id=UUID.randomUUID();
    try {
      db.update("insert into exam_attempts(id,tenant_id,exam_id,student_id,started_at,status) values(?,?,?,?,?,?)",id,tenant,examId,req.studentId(),LocalDateTime.now(),"IN_PROGRESS");
    } catch (org.springframework.dao.DuplicateKeyException ex) {
      UUID concurrent=findAttemptForUpdate(examId,req.studentId(),tenant);
      if(concurrent!=null) return ResponseEntity.ok(Map.of("attemptId",concurrent,"status","IN_PROGRESS"));
      throw ex;
    }
    return ResponseEntity.ok(Map.of("attemptId",id,"status","IN_PROGRESS"));
  }

  @PostMapping("/attempts/{attemptId}/submit")
  @org.springframework.transaction.annotation.Transactional
  @PreAuthorize("hasAuthority('exams.manage') or hasRole('STUDENT')")
  public ResponseEntity<?> submit(@PathVariable UUID attemptId,@RequestBody SubmitAttempt req, Authentication auth){
    UUID tenant=TenantContextHolder.getRequired();
    if(req==null || req.answers()==null) throw new IllegalArgumentException("Answers are required");
    Map<String,Object> attempt=db.queryForMap("select exam_id,student_id,status from exam_attempts where id=? and tenant_id=? for update",attemptId,tenant);
    if(!"IN_PROGRESS".equals(attempt.get("status"))) return ResponseEntity.badRequest().body(Map.of("code","ATTEMPT_CLOSED","message","Attempt is already submitted"));
    UUID studentId=(UUID)attempt.get("student_id"); access.requireOwnStudent(tenant, studentId, auth); UUID examId=(UUID)attempt.get("exam_id"); double total=0;
    for(var e:req.answers().entrySet()){
      UUID q=UUID.fromString(e.getKey()); String answer=e.getValue()==null?"":String.valueOf(e.getValue()).trim();
      Map<String,Object> qrow;
      try{qrow=db.queryForMap("select q.marks,q.question_type,q.subject_id,es.negative_marking from exam_questions eq join questions q on q.id=eq.question_id and q.tenant_id=eq.tenant_id join exam_subjects es on es.exam_id=eq.exam_id and es.subject_id=eq.subject_id where eq.exam_id=? and eq.question_id=? and eq.tenant_id=?",examId,q,tenant);}catch(Exception ex){throw new IllegalArgumentException("Question is not part of this exam");}
      List<Map<String,Object>> options=db.queryForList("select qo.option_text,qo.is_correct from question_options qo join questions q on q.id=qo.question_id and q.tenant_id=? where qo.question_id=? order by qo.display_order",tenant,q);
      boolean correct=grade(String.valueOf(qrow.get("question_type")),answer,options);
      BigDecimal marks=(BigDecimal)qrow.get("marks"); BigDecimal configuredNegative=(BigDecimal)qrow.get("negative_marking"); double awarded=correct?marks.doubleValue():(configuredNegative != null && configuredNegative.signum()>0 ? -configuredNegative.doubleValue() : 0);
      db.update("insert into attempt_answers(id,attempt_id,question_id,answer_text,is_correct,marks_awarded) values(?,?,?,?,?,?) on conflict (attempt_id,question_id) do update set answer_text=excluded.answer_text,is_correct=excluded.is_correct,marks_awarded=excluded.marks_awarded",UUID.randomUUID(),attemptId,q,answer,correct,awarded);
      total+=awarded;
    }
    db.update("update exam_attempts set score=?,submitted_at=?,status='SUBMITTED' where id=? and tenant_id=?",total,LocalDateTime.now(),attemptId,tenant);
    return ResponseEntity.ok(Map.of("attemptId",attemptId,"score",total,"status","SUBMITTED"));
  }

  @GetMapping("/{examId}/attempts/{attemptId}")
  @PreAuthorize("hasRole('STUDENT')")
  public Map<String,Object> attempt(@PathVariable UUID examId,@PathVariable UUID attemptId,Authentication auth){
    UUID tenant=TenantContextHolder.getRequired();
    Map<String,Object> a;
    try{
      a=db.queryForMap("select id,exam_id,student_id,started_at,submitted_at,score,status from exam_attempts where id=? and exam_id=? and tenant_id=?",attemptId,examId,tenant);
    }catch(Exception e){throw new IllegalArgumentException("Attempt not found");}
    UUID studentId=(UUID)a.get("student_id"); access.requireOwnStudent(tenant,studentId,auth);
    Map<String,Object> exam=exam(examId,tenant);
    List<Map<String,Object>> questions=db.queryForList("""
      select q.id,q.subject_id,q.text,q.question_type,q.marks,eq.display_order
      from exam_questions eq join questions q on q.id=eq.question_id and q.tenant_id=eq.tenant_id
      where eq.exam_id=? and eq.tenant_id=? order by eq.display_order
      """,examId,tenant);
    for(Map<String,Object> q:questions){
      q.put("options",db.queryForList("select id,option_text,display_order from question_options where question_id=? order by display_order",q.get("id")));
    }
    Map<String,Object> response=new LinkedHashMap<>();
    response.put("attempt",a); response.put("exam",exam); response.put("questions",questions);
    return response;
  }

  @GetMapping("/{examId}/analysis")
  @PreAuthorize("hasAnyRole('INSTITUTE_OWNER','INSTITUTE_ADMIN','TEACHER')")
  public Map<String,Object> analysis(@PathVariable UUID examId){
    UUID t=TenantContextHolder.getRequired();
    Map<String,Object> row=db.queryForMap("select count(*) attempts, coalesce(avg(score),0) average_score, coalesce(max(score),0) highest_score from exam_attempts where tenant_id=? and exam_id=? and status='SUBMITTED'",t,examId);
    return row;
  }
  private Map<String,Object> exam(UUID id,UUID tenant){try{return db.queryForMap("select status,starts_at,ends_at from exams where id=? and tenant_id=?",id,tenant);}catch(Exception e){throw new IllegalArgumentException("Exam not found");}}
  private int count(String sql,Object... args){Integer n=db.queryForObject(sql,Integer.class,args);return n==null?0:n;}
  private UUID findAttemptForUpdate(UUID exam,UUID student,UUID tenant){List<UUID> x=db.query("select id from exam_attempts where exam_id=? and student_id=? and tenant_id=? and status='IN_PROGRESS' for update",(rs,n)->(UUID)rs.getObject(1),exam,student,tenant);return x.isEmpty()?null:x.get(0);}
  private boolean grade(String type,String answer,List<Map<String,Object>> options){
    String a=answer.trim();
    if(type.equals("MCQ_SINGLE")||type.equals("TRUE_FALSE")) return options.stream().anyMatch(o->Boolean.TRUE.equals(o.get("is_correct")) && a.equalsIgnoreCase(String.valueOf(o.get("option_text"))));
    if(type.equals("MCQ_MULTI")||type.equals("MATCH")){Set<String> given=new TreeSet<>(String.CASE_INSENSITIVE_ORDER);given.addAll(Arrays.asList(a.split(",")));Set<String> correct=new TreeSet<>(String.CASE_INSENSITIVE_ORDER);options.stream().filter(o->Boolean.TRUE.equals(o.get("is_correct"))).forEach(o->correct.add(String.valueOf(o.get("option_text"))));return given.equals(correct);}
    if(type.equals("FILL_BLANK")) return options.stream().filter(o->Boolean.TRUE.equals(o.get("is_correct"))).anyMatch(o->a.equalsIgnoreCase(String.valueOf(o.get("option_text"))));
    return false;
  }
  private LocalDateTime timestamp(Object value){
    if(value==null) return null;
    if(value instanceof LocalDateTime v) return v;
    if(value instanceof java.sql.Timestamp v) return v.toLocalDateTime();
    if(value instanceof java.time.OffsetDateTime v) return v.toLocalDateTime();
    if(value instanceof java.time.Instant v) return LocalDateTime.ofInstant(v,java.time.ZoneId.systemDefault());
    throw new IllegalArgumentException("Unsupported exam timestamp");
  }
  private double negative(double marks,boolean enabled){return enabled?-marks:0;}
  public record StartAttempt(UUID studentId){}
  public record SubmitAttempt(Map<String,Object> answers){}
}
