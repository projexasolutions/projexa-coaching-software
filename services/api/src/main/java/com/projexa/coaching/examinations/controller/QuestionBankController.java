package com.projexa.coaching.examinations.controller;
import com.projexa.coaching.common.tenant.TenantContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/v1/questions")
public class QuestionBankController {
 private final JdbcTemplate db; public QuestionBankController(JdbcTemplate db){this.db=db;}
 @GetMapping @PreAuthorize("hasAnyAuthority('questions.manage','exams.manage')")
 public List<Map<String,Object>> list(@RequestParam(required=false) UUID subjectId,@RequestParam(required=false) String type){
  UUID t=TenantContextHolder.getRequired();
  if(subjectId!=null&&!exists("select count(*) from subjects where id=? and tenant_id=?",subjectId,t)) throw new IllegalArgumentException("Subject is invalid for this institute");
  if(subjectId!=null&&type!=null)return db.queryForList("select * from questions where tenant_id=? and subject_id=? and question_type=? and active=true order by created_at desc",t,subjectId,type);
  if(subjectId!=null)return db.queryForList("select * from questions where tenant_id=? and subject_id=? and active=true order by created_at desc",t,subjectId);
  return db.queryForList("select * from questions where tenant_id=? and active=true order by created_at desc",t);
 }
 @GetMapping("/{id}") @PreAuthorize("hasAnyAuthority('questions.manage','exams.manage')")
 public Map<String,Object> get(@PathVariable UUID id){UUID t=TenantContextHolder.getRequired();Map<String,Object> q=db.queryForMap("select * from questions where id=? and tenant_id=? and active=true",id,t);q.put("options",db.queryForList("select id,option_text text,is_correct correct,display_order from question_options where question_id=? order by display_order",id));return q;}
 @PostMapping @PreAuthorize("hasAuthority('questions.manage')")
 public Map<String,Object> create(@RequestBody Question q){UUID t=TenantContextHolder.getRequired();validate(q,t);UUID id=UUID.randomUUID();db.update("insert into questions(id,tenant_id,subject_id,text,question_type,difficulty,topic,chapter,marks,active) values(?,?,?,?,?,?,?,?,?,true)",id,t,q.subjectId(),q.text().trim(),type(q),q.difficulty(),q.topic(),q.chapter(),q.marks());replaceOptions(id,q.options());return get(id);}
 @PutMapping("/{id}") @PreAuthorize("hasAuthority('questions.manage')")
 public Map<String,Object> update(@PathVariable UUID id,@RequestBody Question q){UUID t=TenantContextHolder.getRequired();require(id,t);validate(q,t);db.update("update questions set subject_id=?,text=?,question_type=?,difficulty=?,topic=?,chapter=?,marks=? where id=? and tenant_id=?",q.subjectId(),q.text().trim(),type(q),q.difficulty(),q.topic(),q.chapter(),q.marks(),id,t);db.update("delete from question_options where question_id=?",id);replaceOptions(id,q.options());return get(id);}
 @DeleteMapping("/{id}") @PreAuthorize("hasAuthority('questions.manage')") public void archive(@PathVariable UUID id){UUID t=TenantContextHolder.getRequired();require(id,t);db.update("update questions set active=false where id=? and tenant_id=?",id,t);}
 private void validate(Question q,UUID t){if(q==null||q.subjectId()==null)throw new IllegalArgumentException("Subject is required");if(q.text()==null||q.text().isBlank())throw new IllegalArgumentException("Question text is required");if(q.marks()<=0)throw new IllegalArgumentException("Marks must be greater than zero");if(!exists("select count(*) from subjects where id=? and tenant_id=?",q.subjectId(),t))throw new IllegalArgumentException("Subject is invalid for this institute");String ty=type(q);Set<String> a=Set.of("MCQ_SINGLE","MCQ_MULTI","TRUE_FALSE","FILL_BLANK","MATCH","ASSERTION_REASON","IMAGE","CASE_PASSAGE");if(!a.contains(ty))throw new IllegalArgumentException("Unsupported question type");if(q.options()!=null)for(Option o:q.options())if(o.text()==null||o.text().isBlank())throw new IllegalArgumentException("Option text is required");if(ty.equals("MCQ_SINGLE")&&(q.options()==null||q.options().stream().filter(Option::correct).count()!=1))throw new IllegalArgumentException("MCQ_SINGLE requires exactly one correct option");if(ty.equals("MCQ_MULTI")&&(q.options()==null||q.options().stream().noneMatch(Option::correct)))throw new IllegalArgumentException("MCQ_MULTI requires a correct option");if(ty.equals("TRUE_FALSE")&&(q.options()==null||q.options().size()!=2||q.options().stream().filter(Option::correct).count()!=1))throw new IllegalArgumentException("TRUE_FALSE requires exactly two options with one correct option");if(ty.equals("FILL_BLANK")&&(q.options()==null||q.options().stream().filter(Option::correct).count()<1))throw new IllegalArgumentException("FILL_BLANK requires at least one accepted correct answer");if(ty.equals("MATCH")&&(q.options()==null||q.options().size()<2))throw new IllegalArgumentException("MATCH requires at least two options");}
 private String type(Question q){return q.questionType()==null||q.questionType().isBlank()?"MCQ_SINGLE":q.questionType().trim().toUpperCase();}
 private void replaceOptions(UUID id,List<Option> o){if(o==null)return;for(int i=0;i<o.size();i++){Option x=o.get(i);db.update("insert into question_options(id,question_id,option_text,is_correct,display_order) values(?,?,?,?,?)",UUID.randomUUID(),id,x.text().trim(),x.correct(),i);}}
 private void require(UUID id,UUID t){if(!exists("select count(*) from questions where id=? and tenant_id=? and active=true",id,t))throw new IllegalArgumentException("Question not found");}
 private boolean exists(String sql,Object...args){Integer n=db.queryForObject(sql,Integer.class,args);return n!=null&&n>0;}
 public record Question(UUID subjectId,String text,String questionType,String difficulty,String topic,String chapter,double marks,List<Option> options){}
 public record Option(String text,boolean correct){}
}