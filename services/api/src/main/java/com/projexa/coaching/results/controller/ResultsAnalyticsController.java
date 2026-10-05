package com.projexa.coaching.results.controller;

import com.projexa.coaching.common.security.ResourceAccess;
import com.projexa.coaching.common.tenant.TenantContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/results")
public class ResultsAnalyticsController {
  private final JdbcTemplate db;
  private final ResourceAccess access;
  public ResultsAnalyticsController(JdbcTemplate db,ResourceAccess access){this.db=db;this.access=access;}

  @GetMapping("/exams/{examId}/dashboard")
  @PreAuthorize("hasAuthority('results.read') or hasAuthority('results.manage') or hasAuthority('dashboard.read')")
  public Map<String,Object> dashboard(@PathVariable UUID examId){
    UUID t=TenantContextHolder.getRequired(); exam(t,examId);
    Map<String,Object> summary=db.queryForMap("""
      select count(*) students,coalesce(avg(obtained_marks),0) average_marks,coalesce(avg(percentage),0) average_percentage,
             coalesce(max(percentage),0) highest_percentage,
             count(*) filter(where exists (
               select 1 from result_subjects rs
               where rs.result_id=r.id
             ) and not exists (
               select 1 from result_subjects rs
               join exam_subjects es on es.tenant_id=? and es.exam_id=r.exam_id and es.subject_id=rs.subject_id
               where rs.result_id=r.id and rs.obtained_marks < coalesce(es.pass_marks,0)
             )) pass_count,
             count(*) filter(where not exists (
               select 1 from result_subjects rs where rs.result_id=r.id
             ) or exists (
               select 1 from result_subjects rs
               join exam_subjects es on es.tenant_id=? and es.exam_id=r.exam_id and es.subject_id=rs.subject_id
               where rs.result_id=r.id and rs.obtained_marks < coalesce(es.pass_marks,0)
             )) fail_count
      from results where tenant_id=? and exam_id=?
      """,t,t,t,examId);
    return Map.of("summary",summary,"subjects",db.queryForList("""
      select rs.subject_id,s.name subject_name,count(*) students,coalesce(avg(rs.obtained_marks),0) average_marks,
             coalesce(max(rs.obtained_marks),0) highest_marks
      from result_subjects rs join results r on r.id=rs.result_id and r.tenant_id=?
      join subjects s on s.id=rs.subject_id and s.tenant_id=?
      where r.exam_id=? group by rs.subject_id,s.name order by s.name
      """,t,t,examId));
  }

  @GetMapping("/exams/{examId}/students")
  @PreAuthorize("hasAuthority('results.read') or hasAuthority('results.manage') or hasAuthority('dashboard.read')")
  public List<Map<String,Object>> students(@PathVariable UUID examId){
    UUID t=TenantContextHolder.getRequired(); exam(t,examId);
    return db.queryForList("""
      select r.student_id,st.admission_number,st.first_name,st.last_name,r.total_marks,r.obtained_marks,r.percentage,r.rank,r.status
      from results r join students st on st.id=r.student_id and st.tenant_id=r.tenant_id
      where r.tenant_id=? and r.exam_id=? order by r.rank nulls last,st.first_name,st.last_name
      """,t,examId);
  }

  @GetMapping("/students/{studentId}/trend")
  @PreAuthorize("hasAuthority('results.read') or hasAuthority('results.manage') or hasRole('STUDENT')")
  public List<Map<String,Object>> trend(@PathVariable UUID studentId,Authentication auth){
    UUID t=TenantContextHolder.getRequired(); access.requireOwnStudent(t,studentId,auth);
    return db.queryForList("""
      select r.exam_id,e.name exam_name,e.exam_type,e.starts_at,r.total_marks,r.obtained_marks,r.percentage,r.rank,r.published_at
      from results r join exams e on e.id=r.exam_id and e.tenant_id=r.tenant_id
      where r.tenant_id=? and r.student_id=? and r.status='PUBLISHED'
      order by coalesce(e.starts_at,r.published_at) asc
      """,t,studentId);
  }

  @PostMapping("/exams/{examId}/generate")
  @Transactional
  @PreAuthorize("hasAuthority('results.manage') or hasRole('INSTITUTE_OWNER')")
  public Map<String,Object> generate(@PathVariable UUID examId){
    UUID t=TenantContextHolder.getRequired(); exam(t,examId);
    Double maxValue=db.queryForObject("select coalesce(sum(max_marks),0) from exam_subjects where tenant_id=? and exam_id=?",Double.class,t,examId); double max=maxValue==null?0:maxValue;
    if(max<=0) throw new IllegalArgumentException("Configure exam subjects and maximum marks before generating results");
    List<Map<String,Object>> attempts=db.queryForList("select id attempt_id,student_id,score from exam_attempts where tenant_id=? and exam_id=? and status='SUBMITTED'",t,examId);
    attempts.sort((a,b)->Double.compare(number(b.get("score")),number(a.get("score"))));
    double previous=Double.NaN; int rank=0;
    for(int i=0;i<attempts.size();i++){
      double score=number(attempts.get(i).get("score"));
      if(score<0 || score>max) throw new IllegalArgumentException("Attempt score is outside the exam maximum marks");
      if(Double.isNaN(previous)||Double.compare(score,previous)!=0) rank=i+1;
      previous=score;
      UUID student=(UUID)attempts.get(i).get("student_id");
      UUID attemptId=(UUID)attempts.get(i).get("attempt_id");
      double pct=Math.round((score*10000.0/max))/100.0;
      UUID resultId=UUID.randomUUID();
      db.update("""
        insert into results(id,tenant_id,exam_id,student_id,total_marks,obtained_marks,percentage,rank,status)
        values(?,?,?,?,?,?,?,?, 'DRAFT')
        on conflict(exam_id,student_id) do update set total_marks=excluded.total_marks,obtained_marks=excluded.obtained_marks,percentage=excluded.percentage,rank=excluded.rank,status='DRAFT',published_at=null
        """,resultId,t,examId,student,max,score,pct,rank);
      UUID rid=db.queryForObject("select id from results where tenant_id=? and exam_id=? and student_id=?",UUID.class,t,examId,student);
      db.update("delete from result_subjects where result_id=?",rid);
      List<Map<String,Object>> subjectMarks=db.queryForList("""
        select q.subject_id,coalesce(sum(aa.marks_awarded),0) obtained
        from attempt_answers aa join questions q on q.id=aa.question_id and q.tenant_id=?
        where aa.attempt_id=? group by q.subject_id
        """,t,attemptId);
      for(Map<String,Object> sm:subjectMarks){
        UUID subjectId=(UUID)sm.get("subject_id");
        Double subjectMax=db.queryForObject("select max_marks from exam_subjects where tenant_id=? and exam_id=? and subject_id=?",Double.class,t,examId,subjectId);
        double subjectObtained=number(sm.get("obtained"));
        if(subjectMax==null) throw new IllegalArgumentException("Result contains a subject not configured for this exam");
        if(subjectObtained<0 || subjectObtained>subjectMax) throw new IllegalArgumentException("Subject result exceeds maximum marks");
        db.update("insert into result_subjects(id,result_id,subject_id,max_marks,obtained_marks) values(?,?,?,?,?)",UUID.randomUUID(),rid,subjectId,subjectMax,subjectObtained);
      }
    }
    return Map.of("examId",examId,"generated",attempts.size(),"maxMarks",max);
  }

  @PostMapping("/exams/{examId}/publish")
  @Transactional
  @PreAuthorize("hasAuthority('results.manage') or hasRole('INSTITUTE_OWNER')")
  public Map<String,Object> publish(@PathVariable UUID examId){
    UUID t=TenantContextHolder.getRequired();
    Map<String,Object> examRow=db.queryForMap("select status,ends_at from exams where id=? and tenant_id=? for update",examId,t);
    String examStatus=String.valueOf(examRow.get("status"));
    if(!Set.of("SCHEDULED","LIVE").contains(examStatus)) throw new IllegalArgumentException("Results can only be published for an open or live exam");
    Object endsAt=examRow.get("ends_at");
    if(endsAt instanceof LocalDateTime dt && LocalDateTime.now().isBefore(dt)) throw new IllegalArgumentException("Exam has not ended yet");
    if(endsAt instanceof java.sql.Timestamp ts && LocalDateTime.now().isBefore(ts.toLocalDateTime())) throw new IllegalArgumentException("Exam has not ended yet");
    int draft=count("select count(*) from results where tenant_id=? and exam_id=? and status='DRAFT'",t,examId);
    if(draft==0) throw new IllegalArgumentException("No generated draft results to publish");
    int n=db.update("update results set status='PUBLISHED',published_at=? where tenant_id=? and exam_id=? and status='DRAFT'",LocalDateTime.now(),t,examId);
    db.update("update exams set status='COMPLETED' where tenant_id=? and id=?",t,examId);
    return Map.of("published",n);
  }

  @GetMapping("/students/{studentId}")
  @PreAuthorize("hasAuthority('results.read') or hasAuthority('results.manage') or hasRole('STUDENT')")
  public List<Map<String,Object>> student(@PathVariable UUID studentId,Authentication auth){
    UUID t=TenantContextHolder.getRequired(); access.requireOwnStudent(t,studentId,auth);
    return db.queryForList("""
      select r.exam_id,r.obtained_marks,r.total_marks,r.percentage,r.rank,r.status,e.name exam_name,e.exam_type,e.starts_at
      from results r join exams e on e.id=r.exam_id
      where r.tenant_id=? and r.student_id=? and r.status='PUBLISHED' order by coalesce(e.starts_at,r.published_at) desc
      """,t,studentId);
  }

  private Map<String,Object> exam(UUID t,UUID id){try{return db.queryForMap("select id,name from exams where id=? and tenant_id=?",id,t);}catch(Exception e){throw new IllegalArgumentException("Exam not found");}}
  private int count(String sql,Object... args){Integer n=db.queryForObject(sql,Integer.class,args);return n==null?0:n;}
  private double number(Object v){return v==null?0:((Number)v).doubleValue();}
}