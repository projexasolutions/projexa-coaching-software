package com.projexa.coaching.homework.controller;

import com.projexa.coaching.common.tenant.TenantContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/homework")
public class HomeworkController {
  private final JdbcTemplate db;
  public HomeworkController(JdbcTemplate db){this.db=db;}

  @GetMapping
  @PreAuthorize("hasAuthority('homework.manage') or hasAuthority('dashboard.read')")
  public List<Map<String,Object>> list(@RequestParam(required=false) UUID batchId){
    UUID t=TenantContextHolder.getRequired();
    if(batchId!=null) requireBatch(t,batchId);
    if(batchId==null) return db.queryForList("""
      select a.id,a.batch_id,a.teacher_id,a.title,a.description,a.due_at,a.created_at,b.name batch_name,
             trim(coalesce(te.first_name,'')||' '||coalesce(te.last_name,'')) teacher_name,
             (select count(*) from assignment_submissions s where s.assignment_id=a.id) submission_count
      from assignments a join batches b on b.id=a.batch_id and b.tenant_id=a.tenant_id
      left join teachers te on te.id=a.teacher_id and te.tenant_id=a.tenant_id
      where a.tenant_id=? order by coalesce(a.due_at,'9999-12-31'::timestamp) desc,a.created_at desc
      """,t);
    return db.queryForList("""
      select a.id,a.batch_id,a.teacher_id,a.title,a.description,a.due_at,a.created_at,b.name batch_name,
             trim(coalesce(te.first_name,'')||' '||coalesce(te.last_name,'')) teacher_name,
             (select count(*) from assignment_submissions s where s.assignment_id=a.id) submission_count
      from assignments a join batches b on b.id=a.batch_id and b.tenant_id=a.tenant_id
      left join teachers te on te.id=a.teacher_id and te.tenant_id=a.tenant_id
      where a.tenant_id=? and a.batch_id=? order by coalesce(a.due_at,'9999-12-31'::timestamp) desc,a.created_at desc
      """,t,batchId);
  }

  @PostMapping
  @PreAuthorize("hasAuthority('homework.manage')")
  public Map<String,Object> create(@RequestBody HomeworkRequest r){
    UUID t=TenantContextHolder.getRequired(); validate(r); requireBatch(t,r.batchId());
    if(r.teacherId()!=null) requireTeacher(t,r.teacherId());
    if(r.teacherId()!=null && count("select count(*) from teacher_batch_assignments where teacher_id=? and batch_id=?",r.teacherId(),r.batchId())==0)
      throw new IllegalArgumentException("Teacher is not assigned to this batch");
    UUID id=UUID.randomUUID();
    db.update("insert into assignments(id,tenant_id,batch_id,teacher_id,title,description,due_at) values(?,?,?,?,?,?,?)",
      id,t,r.batchId(),r.teacherId(),r.title().trim(),trim(r.description()),r.dueAt());
    return get(t,id);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('homework.manage')")
  public Map<String,Object> update(@PathVariable UUID id,@RequestBody HomeworkRequest r){
    UUID t=TenantContextHolder.getRequired(); requireAssignment(t,id); validate(r); requireBatch(t,r.batchId());
    if(r.teacherId()!=null) requireTeacher(t,r.teacherId());
    if(r.teacherId()!=null && count("select count(*) from teacher_batch_assignments where teacher_id=? and batch_id=?",r.teacherId(),r.batchId())==0)
      throw new IllegalArgumentException("Teacher is not assigned to this batch");
    db.update("update assignments set batch_id=?,teacher_id=?,title=?,description=?,due_at=? where id=? and tenant_id=?",
      r.batchId(),r.teacherId(),r.title().trim(),trim(r.description()),r.dueAt(),id,t);
    return get(t,id);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('homework.manage')")
  public Map<String,Object> delete(@PathVariable UUID id){
    UUID t=TenantContextHolder.getRequired(); requireAssignment(t,id);
    db.update("delete from assignments where id=? and tenant_id=?",id,t);
    return Map.of("deleted",true,"id",id);
  }

  @GetMapping("/{id}/submissions")
  @PreAuthorize("hasAuthority('homework.manage') or hasAuthority('dashboard.read')")
  public List<Map<String,Object>> submissions(@PathVariable UUID id){
    UUID t=TenantContextHolder.getRequired(); Map<String,Object> a=get(t,id);
    return db.queryForList("""
      select st.id student_id,st.admission_number,st.first_name,st.last_name,
             s.submitted_at,s.status,s.score,s.feedback
      from students st join enrollments e on e.student_id=st.id and e.tenant_id=st.tenant_id
      left join assignment_submissions s on s.assignment_id=? and s.student_id=st.id and s.tenant_id=st.tenant_id
      where st.tenant_id=? and e.batch_id=? and e.status='ACTIVE'
      order by st.first_name,st.last_name
      """,id,t,a.get("batch_id"));
  }

  @PutMapping("/{id}/submissions/{studentId}")
  @PreAuthorize("hasAuthority('homework.manage')")
  public Map<String,Object> grade(@PathVariable UUID id,@PathVariable UUID studentId,@RequestBody SubmissionRequest r){
    UUID t=TenantContextHolder.getRequired(); Map<String,Object> a=get(t,id);
    if(count("select count(*) from enrollments where tenant_id=? and student_id=? and batch_id=? and status='ACTIVE'",t,studentId,a.get("batch_id"))==0)
      throw new IllegalArgumentException("Student is not actively enrolled in this homework batch");
    String status=r==null||r.status()==null?"SUBMITTED":r.status().trim().toUpperCase();
    if(!Set.of("PENDING","SUBMITTED","GRADED","LATE").contains(status)) throw new IllegalArgumentException("Unsupported submission status");
    if(r!=null && r.score()!=null && r.score()<0) throw new IllegalArgumentException("Score cannot be negative");
    db.update("""
      insert into assignment_submissions(id,tenant_id,assignment_id,student_id,submitted_at,status,score,feedback)
      values(?,?,?,?,?,?,?,?,?)
      on conflict(assignment_id,student_id) do update set submitted_at=excluded.submitted_at,status=excluded.status,score=excluded.score,feedback=excluded.feedback
      """,UUID.randomUUID(),t,id,studentId,r!=null&&r.submittedAt()!=null?r.submittedAt():LocalDateTime.now(),status,r==null?null:r.score(),r==null?null:trim(r.feedback()));
    return db.queryForMap("select assignment_id,student_id,submitted_at,status,score,feedback from assignment_submissions where assignment_id=? and student_id=?",id,studentId);
  }

  @GetMapping("/resources")
  @PreAuthorize("hasAuthority('homework.manage') or hasAuthority('dashboard.read')")
  public Map<String,Object> resources(){
    UUID t=TenantContextHolder.getRequired();
    return Map.of(
      "batches",db.queryForList("select id,name,code from batches where tenant_id=? and status='ACTIVE' order by name",t),
      "teachers",db.queryForList("select id,first_name,last_name from teachers where tenant_id=? and status='ACTIVE' order by first_name,last_name",t)
    );
  }

  private Map<String,Object> get(UUID t,UUID id){try{return db.queryForMap("select id,batch_id,teacher_id,title,description,due_at,created_at from assignments where id=? and tenant_id=?",id,t);}catch(Exception e){throw new IllegalArgumentException("Homework not found");}}
  private void requireAssignment(UUID t,UUID id){get(t,id);}
  private void requireBatch(UUID t,UUID id){if(id==null||count("select count(*) from batches where id=? and tenant_id=? and status='ACTIVE'",id,t)==0)throw new IllegalArgumentException("Batch is invalid for this institute");}
  private void requireTeacher(UUID t,UUID id){if(count("select count(*) from teachers where id=? and tenant_id=? and status='ACTIVE'",id,t)==0)throw new IllegalArgumentException("Teacher is invalid for this institute");}
  private void validate(HomeworkRequest r){if(r==null||r.batchId()==null)throw new IllegalArgumentException("Batch is required");if(r.title()==null||r.title().trim().isEmpty())throw new IllegalArgumentException("Title is required");}
  private String trim(String s){if(s==null)return null;String v=s.trim();return v.isEmpty()?null:v;}
  private int count(String sql,Object... args){Integer n=db.queryForObject(sql,Integer.class,args);return n==null?0:n;}

  public record HomeworkRequest(UUID batchId,UUID teacherId,String title,String description,LocalDateTime dueAt){}
  public record SubmissionRequest(LocalDateTime submittedAt,String status,Double score,String feedback){}
}
