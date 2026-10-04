package com.projexa.coaching.learning.controller;

import com.projexa.coaching.common.tenant.TenantContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/learning/resources")
public class LearningResourceController {
  private final JdbcTemplate db;
  public LearningResourceController(JdbcTemplate db){this.db=db;}

  @GetMapping
  @PreAuthorize("hasAuthority('learning.read') or hasAuthority('learning.manage') or hasAuthority('dashboard.read')")
  public List<Map<String,Object>> list(@RequestParam(required=false) UUID batchId,
                                       @RequestParam(required=false) UUID subjectId,
                                       @RequestParam(required=false) Boolean published){
    UUID tenant=TenantContextHolder.getRequired();
    if(batchId!=null) requireBatch(tenant,batchId);
    if(subjectId!=null) requireSubject(tenant,subjectId);
    StringBuilder sql=new StringBuilder("""
      select lr.id,lr.batch_id,lr.subject_id,lr.teacher_id,lr.title,lr.description,
             lr.resource_type,lr.resource_url,lr.topic,lr.published,lr.created_at,
             b.name batch_name,s.name subject_name,
             trim(coalesce(t.first_name,'')||' '||coalesce(t.last_name,'')) teacher_name
      from learning_resources lr
      left join batches b on b.id=lr.batch_id and b.tenant_id=lr.tenant_id
      left join subjects s on s.id=lr.subject_id and s.tenant_id=lr.tenant_id
      left join teachers t on t.id=lr.teacher_id and t.tenant_id=lr.tenant_id
      where lr.tenant_id=?
      """);
    List<Object> args=new ArrayList<>(List.of(tenant));
    if(batchId!=null){sql.append(" and lr.batch_id=?");args.add(batchId);}
    if(subjectId!=null){sql.append(" and lr.subject_id=?");args.add(subjectId);}
    if(published!=null){sql.append(" and lr.published=?");args.add(published);}
    sql.append(" order by lr.created_at desc");
    return db.queryForList(sql.toString(),args.toArray());
  }

  @PostMapping
  @PreAuthorize("hasAuthority('learning.manage')")
  public Map<String,Object> create(@RequestBody ResourceRequest r){
    UUID tenant=TenantContextHolder.getRequired();
    validate(r); validateRelations(tenant,r);
    UUID id=UUID.randomUUID();
    db.update("""
      insert into learning_resources(id,tenant_id,batch_id,subject_id,teacher_id,title,description,
      resource_type,resource_url,topic,published)
      values(?,?,?,?,?,?,?,?,?,?,?)
      """,id,tenant,r.batchId(),r.subjectId(),r.teacherId(),r.title().trim(),trim(r.description()),
      r.resourceType(),r.resourceUrl().trim(),trim(r.topic()),Boolean.TRUE.equals(r.published()));
    return get(tenant,id);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('learning.manage')")
  public Map<String,Object> update(@PathVariable UUID id,@RequestBody ResourceRequest r){
    UUID tenant=TenantContextHolder.getRequired();
    get(tenant,id); validate(r); validateRelations(tenant,r);
    db.update("""
      update learning_resources set batch_id=?,subject_id=?,teacher_id=?,title=?,description=?,
      resource_type=?,resource_url=?,topic=?,published=? where id=? and tenant_id=?
      """,r.batchId(),r.subjectId(),r.teacherId(),r.title().trim(),trim(r.description()),
      r.resourceType(),r.resourceUrl().trim(),trim(r.topic()),Boolean.TRUE.equals(r.published()),id,tenant);
    return get(tenant,id);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('learning.manage')")
  public Map<String,Object> delete(@PathVariable UUID id){
    UUID tenant=TenantContextHolder.getRequired(); get(tenant,id);
    db.update("delete from learning_resources where id=? and tenant_id=?",id,tenant);
    return Map.of("deleted",true,"id",id);
  }

  @GetMapping("/resources")
  @PreAuthorize("hasAuthority('learning.read') or hasAuthority('learning.manage') or hasAuthority('dashboard.read')")
  public Map<String,Object> resources(){
    UUID tenant=TenantContextHolder.getRequired();
    return Map.of(
      "batches",db.queryForList("select id,name,code from batches where tenant_id=? and status='ACTIVE' order by name",tenant),
      "subjects",db.queryForList("select id,name,code from subjects where tenant_id=? and status='ACTIVE' order by name",tenant),
      "teachers",db.queryForList("select id,first_name,last_name from teachers where tenant_id=? and status='ACTIVE' order by first_name,last_name",tenant)
    );
  }

  private Map<String,Object> get(UUID tenant,UUID id){
    try{return db.queryForMap("select id,batch_id,subject_id,teacher_id,title,description,resource_type,resource_url,topic,published,created_at from learning_resources where id=? and tenant_id=?",id,tenant);}
    catch(Exception e){throw new IllegalArgumentException("Learning resource not found");}
  }
  private void validate(ResourceRequest r){
    if(r==null) throw new IllegalArgumentException("Resource is required");
    if(r.title()==null||r.title().trim().isEmpty()) throw new IllegalArgumentException("Title is required");
    if(r.resourceUrl()==null||r.resourceUrl().trim().isEmpty()) throw new IllegalArgumentException("Resource URL is required");
    String type=r.resourceType()==null?"LINK":r.resourceType().trim().toUpperCase();
    if(!Set.of("LINK","PDF","VIDEO","DOCUMENT","RECORDING").contains(type)) throw new IllegalArgumentException("Unsupported resource type");
  }
  private void validateRelations(UUID tenant,ResourceRequest r){
    if(r.batchId()!=null && count("select count(*) from batches where id=? and tenant_id=? and status='ACTIVE'",r.batchId(),tenant)==0) throw new IllegalArgumentException("Batch is invalid for this institute");
    if(r.subjectId()!=null && count("select count(*) from subjects where id=? and tenant_id=? and status='ACTIVE'",r.subjectId(),tenant)==0) throw new IllegalArgumentException("Subject is invalid for this institute");
    if(r.teacherId()!=null && count("select count(*) from teachers where id=? and tenant_id=? and status='ACTIVE'",r.teacherId(),tenant)==0) throw new IllegalArgumentException("Teacher is invalid for this institute");
  }
  private void requireBatch(UUID tenant,UUID id){if(count("select count(*) from batches where id=? and tenant_id=?",id,tenant)==0)throw new IllegalArgumentException("Batch is invalid for this institute");}
  private void requireSubject(UUID tenant,UUID id){if(count("select count(*) from subjects where id=? and tenant_id=?",id,tenant)==0)throw new IllegalArgumentException("Subject is invalid for this institute");}
  private int count(String sql,Object... args){Integer n=db.queryForObject(sql,Integer.class,args);return n==null?0:n;}
  private String trim(String s){if(s==null)return null;String v=s.trim();return v.isEmpty()?null:v;}

  public record ResourceRequest(UUID batchId,UUID subjectId,UUID teacherId,String title,String description,
                                String resourceType,String resourceUrl,String topic,Boolean published){}
}
