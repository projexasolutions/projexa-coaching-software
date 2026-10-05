package com.projexa.coaching.examinations.controller;

import com.projexa.coaching.common.tenant.TenantContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/exams")
public class ExamManagementController {
  private final JdbcTemplate db;
  public ExamManagementController(JdbcTemplate db){this.db=db;}

  @GetMapping
  @PreAuthorize("hasAuthority('exams.manage') or hasAuthority('exams.read') or hasAuthority('dashboard.read')")
  public List<Map<String,Object>> list(){
    UUID t=TenantContextHolder.getRequired();
    return db.queryForList("""
      select e.id,e.academic_year_id,e.name,e.exam_type,e.starts_at,e.ends_at,e.status,
             ay.name academic_year,
             (select count(*) from exam_subjects es where es.exam_id=e.id) subject_count,
             (select count(*) from exam_questions eq where eq.exam_id=e.id) question_count,
             (select count(*) from exam_attempts ea where ea.exam_id=e.id) attempt_count
      from exams e join academic_years ay on ay.id=e.academic_year_id and ay.tenant_id=e.tenant_id
      where e.tenant_id=? order by coalesce(e.starts_at,'9999-12-31'::timestamp) desc,e.name
      """,t);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('exams.manage') or hasAuthority('exams.read') or hasAuthority('dashboard.read')")
  public Map<String,Object> get(@PathVariable UUID id){return exam(TenantContextHolder.getRequired(),id);}

  @PostMapping
  @PreAuthorize("hasAuthority('exams.manage')")
  public Map<String,Object> create(@RequestBody ExamRequest r){
    UUID t=TenantContextHolder.getRequired(); validate(r); requireYear(t,r.academicYearId()); validateWindow(r.startsAt(),r.endsAt());
    UUID id=UUID.randomUUID();
    db.update("insert into exams(id,tenant_id,academic_year_id,name,exam_type,starts_at,ends_at,status) values(?,?,?,?,?,?,?,?)",
      id,t,r.academicYearId(),r.name().trim(),r.examType().trim().toUpperCase(),r.startsAt(),r.endsAt(),normalizeStatus(r.status()));
    return exam(t,id);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('exams.manage')")
  public Map<String,Object> update(@PathVariable UUID id,@RequestBody ExamRequest r){
    UUID t=TenantContextHolder.getRequired(); Map<String,Object> current=exam(t,id); validate(r); requireYear(t,r.academicYearId()); validateWindow(r.startsAt(),r.endsAt());
    String nextStatus=normalizeStatus(r.status()); ensureTransition(String.valueOf(current.get("status")),nextStatus);
    db.update("update exams set academic_year_id=?,name=?,exam_type=?,starts_at=?,ends_at=?,status=? where id=? and tenant_id=?",
      r.academicYearId(),r.name().trim(),r.examType().trim().toUpperCase(),r.startsAt(),r.endsAt(),nextStatus,id,t);
    return exam(t,id);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('exams.manage')")
  public Map<String,Object> archive(@PathVariable UUID id){
    UUID t=TenantContextHolder.getRequired(); exam(t,id);
    db.update("update exams set status='ARCHIVED' where id=? and tenant_id=?",id,t);
    return Map.of("id",id,"status","ARCHIVED");
  }

  @GetMapping("/{id}/subjects")
  @PreAuthorize("hasAuthority('exams.manage') or hasAuthority('exams.read') or hasAuthority('dashboard.read')")
  public List<Map<String,Object>> subjects(@PathVariable UUID id){
    UUID t=TenantContextHolder.getRequired(); exam(t,id);
    return db.queryForList("select es.id,es.subject_id,s.name subject_name,s.code,max_marks,pass_marks,negative_marking from exam_subjects es join subjects s on s.id=es.subject_id and s.tenant_id=es.tenant_id where es.tenant_id=? and es.exam_id=? order by s.name",t,id);
  }

  @PostMapping("/{id}/subjects")
  @PreAuthorize("hasAuthority('exams.manage')")
  public Map<String,Object> addSubject(@PathVariable UUID id,@RequestBody ExamSubjectRequest r){
    UUID t=TenantContextHolder.getRequired(); ensureConfigurable(t,id); validateSubject(t,r.subjectId()); 
    if(r.maxMarks()==null||r.maxMarks()<=0) throw new IllegalArgumentException("Maximum marks must be greater than zero");
    if(r.passMarks()!=null&&(r.passMarks()<0||r.passMarks()>r.maxMarks())) throw new IllegalArgumentException("Pass marks must be between 0 and maximum marks");
    if(r.negativeMarking()!=null&&r.negativeMarking()<0) throw new IllegalArgumentException("Negative marking cannot be negative");
    db.update("insert into exam_subjects(id,tenant_id,exam_id,subject_id,max_marks,pass_marks,negative_marking) values(?,?,?,?,?,?,?) on conflict(exam_id,subject_id) do update set max_marks=excluded.max_marks,pass_marks=excluded.pass_marks,negative_marking=excluded.negative_marking",
      UUID.randomUUID(),t,id,r.subjectId(),r.maxMarks(),r.passMarks(),r.negativeMarking()==null?0:r.negativeMarking());
    return Map.of("saved",true);
  }

  @DeleteMapping("/{id}/subjects/{subjectId}")
  @PreAuthorize("hasAuthority('exams.manage')")
  public Map<String,Object> removeSubject(@PathVariable UUID id,@PathVariable UUID subjectId){
    UUID t=TenantContextHolder.getRequired(); ensureConfigurable(t,id);
    db.update("delete from exam_subjects where tenant_id=? and exam_id=? and subject_id=?",t,id,subjectId);
    db.update("delete from exam_questions where tenant_id=? and exam_id=? and subject_id=?",t,id,subjectId);
    return Map.of("deleted",true);
  }

  @GetMapping("/{id}/questions")
  @PreAuthorize("hasAuthority('exams.manage') or hasAuthority('exams.read') or hasAuthority('dashboard.read')")
  public List<Map<String,Object>> questions(@PathVariable UUID id){
    UUID t=TenantContextHolder.getRequired(); exam(t,id);
    return db.queryForList("select eq.id link_id,q.id,q.subject_id,s.name subject_name,q.text,q.question_type,q.difficulty,q.topic,q.chapter,q.marks,eq.display_order from exam_questions eq join questions q on q.id=eq.question_id and q.tenant_id=eq.tenant_id left join subjects s on s.id=q.subject_id and s.tenant_id=q.tenant_id where eq.tenant_id=? and eq.exam_id=? order by eq.display_order,q.created_at",t,id);
  }

  @PostMapping("/{id}/questions")
  @Transactional
  @PreAuthorize("hasAuthority('exams.manage')")
  public Map<String,Object> setQuestions(@PathVariable UUID id,@RequestBody QuestionSelection r){
    UUID t=TenantContextHolder.getRequired(); ensureConfigurable(t,id);
    List<UUID> ids=r==null||r.questionIds()==null?List.of():r.questionIds();
    if(new HashSet<>(ids).size()!=ids.size()) throw new IllegalArgumentException("Duplicate questions are not allowed");
    db.update("delete from exam_questions where tenant_id=? and exam_id=?",t,id);
    int order=1;
    for(UUID q:ids){
      Map<String,Object> row;
      try{row=db.queryForMap("select id,subject_id,question_type from questions where id=? and tenant_id=? and active=true",q,t);}catch(Exception e){throw new IllegalArgumentException("Question is invalid for this institute: "+q);}
      UUID subject=(UUID)row.get("subject_id");
      String questionType=String.valueOf(row.get("question_type"));
      if(!Set.of("MCQ_SINGLE","MCQ_MULTI","TRUE_FALSE","FILL_BLANK","MATCH").contains(questionType)) throw new IllegalArgumentException("Question type is not yet supported by the live exam grader: "+questionType);
      if(subject==null||count("select count(*) from exam_subjects where tenant_id=? and exam_id=? and subject_id=?",t,id,subject)==0) throw new IllegalArgumentException("Question subject is not configured for this exam");
      db.update("insert into exam_questions(id,tenant_id,exam_id,question_id,subject_id,display_order) values(?,?,?,?,?,?)",UUID.randomUUID(),t,id,q,subject,order++);
    }
    return Map.of("saved",ids.size());
  }

  @GetMapping("/resources")
  @PreAuthorize("hasAuthority('exams.manage') or hasAuthority('exams.read') or hasAuthority('dashboard.read')")
  public Map<String,Object> resources(){
    UUID t=TenantContextHolder.getRequired();
    return Map.of(
      "academicYears",db.queryForList("select id,name,is_current from academic_years where tenant_id=? order by start_date desc",t),
      "subjects",db.queryForList("select id,name,code from subjects where tenant_id=? and active=true order by name",t),
      "questions",db.queryForList("select id,subject_id,text,question_type,difficulty,topic,chapter,marks from questions where tenant_id=? and active=true order by created_at desc",t)
    );
  }

  private Map<String,Object> exam(UUID t,UUID id){
    try{return db.queryForMap("select id,academic_year_id,name,exam_type,starts_at,ends_at,status from exams where id=? and tenant_id=?",id,t);}
    catch(Exception e){throw new IllegalArgumentException("Exam not found");}
  }
  private void validate(ExamRequest r){if(r==null||r.academicYearId()==null)throw new IllegalArgumentException("Academic year is required");if(r.name()==null||r.name().trim().isEmpty())throw new IllegalArgumentException("Exam name is required");if(r.examType()==null||r.examType().trim().isEmpty())throw new IllegalArgumentException("Exam type is required");}
  private void validateWindow(LocalDateTime s,LocalDateTime e){if(s!=null&&e!=null&&!e.isAfter(s))throw new IllegalArgumentException("Exam end must be after start");}
  private String normalizeStatus(String s){String v=s==null?"DRAFT":s.trim().toUpperCase();if(!Set.of("DRAFT","SCHEDULED","LIVE","COMPLETED","ARCHIVED").contains(v))throw new IllegalArgumentException("Unsupported exam status");return v;}
  private void ensureConfigurable(UUID t,UUID id){ Map<String,Object> e=exam(t,id); String s=String.valueOf(e.get("status")); if(!Set.of("DRAFT","SCHEDULED").contains(s)) throw new IllegalArgumentException("Exam configuration cannot be changed after the exam is LIVE, COMPLETED, or ARCHIVED"); }
  private void ensureTransition(String current,String next){ if(current.equals(next)) return; Map<String,Set<String>> allowed=Map.of("DRAFT",Set.of("SCHEDULED","LIVE","ARCHIVED"),"SCHEDULED",Set.of("LIVE","ARCHIVED"),"LIVE",Set.of("COMPLETED","ARCHIVED"),"COMPLETED",Set.of("ARCHIVED"),"ARCHIVED",Set.of()); if(!allowed.getOrDefault(current,Set.of()).contains(next)) throw new IllegalArgumentException("Invalid exam status transition: "+current+" -> "+next); }
  private void requireYear(UUID t,UUID id){if(count("select count(*) from academic_years where id=? and tenant_id=?",id,t)==0)throw new IllegalArgumentException("Academic year is invalid for this institute");}
  private void validateSubject(UUID t,UUID id){if(id==null||count("select count(*) from subjects where id=? and tenant_id=? and active=true",id,t)==0)throw new IllegalArgumentException("Subject is invalid for this institute");}
  private int count(String sql,Object... args){Integer n=db.queryForObject(sql,Integer.class,args);return n==null?0:n;}
  public record ExamRequest(UUID academicYearId,String name,String examType,LocalDateTime startsAt,LocalDateTime endsAt,String status){}
  public record ExamSubjectRequest(UUID subjectId,Double maxMarks,Double passMarks,Double negativeMarking){}
  public record QuestionSelection(List<UUID> questionIds){}
}