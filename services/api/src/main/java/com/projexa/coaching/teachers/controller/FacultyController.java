package com.projexa.coaching.teachers.controller;

import com.projexa.coaching.common.tenant.TenantContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1")
public class FacultyController {
  private final JdbcTemplate db;

  public FacultyController(JdbcTemplate db){this.db=db;}

  @GetMapping("/teachers")
  @PreAuthorize("hasAuthority('teachers.read') or hasAuthority('dashboard.read')")
  public List<Map<String,Object>> teachers(){
    UUID t=TenantContextHolder.getRequired();
    return db.queryForList("""
      select te.id,te.user_id,te.employee_code,te.first_name,te.last_name,te.email,te.phone,te.status,
             coalesce((select count(*) from teacher_batch_assignments tba where tba.teacher_id=te.id),0) batch_count,
             coalesce((select count(*) from teacher_subjects ts where ts.teacher_id=te.id),0) subject_count
      from teachers te where te.tenant_id=? order by te.first_name,te.last_name
      """,t);
  }

  @GetMapping("/teachers/{id}")
  @PreAuthorize("hasAuthority('teachers.read') or hasAuthority('dashboard.read')")
  public Map<String,Object> teacher(@PathVariable UUID id){
    UUID t=TenantContextHolder.getRequired();
    return teacherRow(t,id);
  }

  @PostMapping("/teachers")
  @PreAuthorize("hasAuthority('teachers.create') or hasAuthority('settings.manage')")
  public Map<String,Object> createTeacher(@RequestBody TeacherRequest r){
    UUID t=TenantContextHolder.getRequired();
    validateTeacher(r);
    if(r.employeeCode()!=null && count("select count(*) from teachers where tenant_id=? and lower(employee_code)=lower(?)",t,r.employeeCode())>0)
      throw new IllegalArgumentException("Employee code already exists");
    if(r.userId()!=null && count("select count(*) from users where id=? and tenant_id=?",r.userId(),t)==0)
      throw new IllegalArgumentException("User account is invalid for this institute");
    UUID id=UUID.randomUUID();
    db.update("insert into teachers(id,tenant_id,user_id,employee_code,first_name,last_name,email,phone,status) values(?,?,?,?,?,?,?,?,?)",
      id,t,r.userId(),r.employeeCode(),r.firstName().trim(),trim(r.lastName()),trim(r.email()),trim(r.phone()),normalizeStatus(r.status()));
    return teacherRow(t,id);
  }

  @PutMapping("/teachers/{id}")
  @PreAuthorize("hasAuthority('teachers.update') or hasAuthority('settings.manage')")
  public Map<String,Object> updateTeacher(@PathVariable UUID id,@RequestBody TeacherRequest r){
    UUID t=TenantContextHolder.getRequired();
    requireTeacher(t,id);
    validateTeacher(r);
    if(r.employeeCode()!=null && count("select count(*) from teachers where tenant_id=? and lower(employee_code)=lower(?) and id<>?",t,r.employeeCode(),id)>0)
      throw new IllegalArgumentException("Employee code already exists");
    if(r.userId()!=null && count("select count(*) from users where id=? and tenant_id=?",r.userId(),t)==0)
      throw new IllegalArgumentException("User account is invalid for this institute");
    db.update("update teachers set user_id=?,employee_code=?,first_name=?,last_name=?,email=?,phone=?,status=?,updated_at=current_timestamp where id=? and tenant_id=?",
      r.userId(),r.employeeCode(),r.firstName().trim(),trim(r.lastName()),trim(r.email()),trim(r.phone()),normalizeStatus(r.status()),id,t);
    return teacherRow(t,id);
  }

  @DeleteMapping("/teachers/{id}")
  @PreAuthorize("hasAuthority('teachers.delete') or hasAuthority('settings.manage')")
  public Map<String,Object> archiveTeacher(@PathVariable UUID id){
    UUID t=TenantContextHolder.getRequired();
    requireTeacher(t,id);
    db.update("update teachers set status='INACTIVE',updated_at=current_timestamp where id=? and tenant_id=?",id,t);
    return Map.of("id",id,"status","INACTIVE");
  }

  @GetMapping("/teachers/{id}/assignments")
  @PreAuthorize("hasAuthority('teachers.read') or hasAuthority('dashboard.read')")
  public Map<String,Object> assignments(@PathVariable UUID id){
    UUID t=TenantContextHolder.getRequired();
    requireTeacher(t,id);
    return Map.of(
      "batches",db.queryForList("select b.id,b.name,b.code from teacher_batch_assignments x join batches b on b.id=x.batch_id where x.teacher_id=? and b.tenant_id=? order by b.name",id,t),
      "subjects",db.queryForList("select s.id,s.name,s.code from teacher_subjects x join subjects s on s.id=x.subject_id where x.teacher_id=? and s.tenant_id=? order by s.name",id,t)
    );
  }

  @PutMapping("/teachers/{id}/assignments")
  @PreAuthorize("hasAuthority('teachers.update') or hasAuthority('settings.manage')")
  public Map<String,Object> saveAssignments(@PathVariable UUID id,@RequestBody AssignmentRequest r){
    UUID t=TenantContextHolder.getRequired();
    requireTeacher(t,id);
    List<UUID> batches=r==null||r.batchIds()==null?List.of():r.batchIds();
    List<UUID> subjects=r==null||r.subjectIds()==null?List.of():r.subjectIds();
    validateIds(t,batches,"batches");
    validateIds(t,subjects,"subjects");
    db.update("delete from teacher_batch_assignments where teacher_id=?",id);
    db.update("delete from teacher_subjects where teacher_id=?",id);
    for(UUID batch:batches) db.update("insert into teacher_batch_assignments(teacher_id,batch_id) values(?,?)",id,batch);
    for(UUID subject:subjects) db.update("insert into teacher_subjects(teacher_id,subject_id) values(?,?)",id,subject);
    return assignments(id);
  }

  @GetMapping("/classrooms")
  @PreAuthorize("hasAuthority('academics.read') or hasAuthority('dashboard.read')")
  public List<Map<String,Object>> classrooms(){
    UUID t=TenantContextHolder.getRequired();
    return db.queryForList("select id,name,room_code,capacity,active from classrooms where tenant_id=? order by name",t);
  }

  @PostMapping("/classrooms")
  @PreAuthorize("hasAuthority('academics.manage') or hasAuthority('settings.manage')")
  public Map<String,Object> createClassroom(@RequestBody ClassroomRequest r){
    UUID t=TenantContextHolder.getRequired(); validateClassroom(r);
    if(count("select count(*) from classrooms where tenant_id=? and lower(name)=lower(?)",t,r.name())>0) throw new IllegalArgumentException("Classroom name already exists");
    UUID id=UUID.randomUUID();
    db.update("insert into classrooms(id,tenant_id,name,room_code,capacity,active) values(?,?,?,?,?,true)",id,t,r.name().trim(),trim(r.roomCode()),r.capacity());
    return classroomRow(t,id);
  }

  @PutMapping("/classrooms/{id}")
  @PreAuthorize("hasAuthority('academics.manage') or hasAuthority('settings.manage')")
  public Map<String,Object> updateClassroom(@PathVariable UUID id,@RequestBody ClassroomRequest r){
    UUID t=TenantContextHolder.getRequired(); requireClassroom(t,id); validateClassroom(r);
    if(count("select count(*) from classrooms where tenant_id=? and lower(name)=lower(?) and id<>?",t,r.name(),id)>0) throw new IllegalArgumentException("Classroom name already exists");
    db.update("update classrooms set name=?,room_code=?,capacity=?,active=? where id=? and tenant_id=?",r.name().trim(),trim(r.roomCode()),r.capacity(),r.active()!=null?r.active():true,id,t);
    return classroomRow(t,id);
  }

  @DeleteMapping("/classrooms/{id}")
  @PreAuthorize("hasAuthority('academics.manage') or hasAuthority('settings.manage')")
  public Map<String,Object> archiveClassroom(@PathVariable UUID id){
    UUID t=TenantContextHolder.getRequired(); requireClassroom(t,id);
    db.update("update classrooms set active=false where id=? and tenant_id=?",id,t);
    return Map.of("id",id,"active",false);
  }

  @GetMapping("/faculty/resources")
  @PreAuthorize("hasAuthority('teachers.read') or hasAuthority('dashboard.read')")
  public Map<String,Object> resources(){
    UUID t=TenantContextHolder.getRequired();
    return Map.of(
      "batches",db.queryForList("select id,name,code from batches where tenant_id=? and status='ACTIVE' order by name",t),
      "subjects",db.queryForList("select id,name,code from subjects where tenant_id=? and active=true order by name",t)
    );
  }

  private void validateIds(UUID t,List<UUID> ids,String label){
    Set<UUID> unique=new LinkedHashSet<>(ids);
    if(unique.size()!=ids.size()) throw new IllegalArgumentException("Duplicate "+label+" are not allowed");
    for(UUID id:unique){
      if(id==null || count("select count(*) from "+label+" where tenant_id=? and id=?",t,id)==0) throw new IllegalArgumentException("One or more "+label+" are invalid for this institute");
    }
  }

  private void validateTeacher(TeacherRequest r){
    if(r==null) throw new IllegalArgumentException("Teacher payload is required");
    if(trim(r.firstName())==null) throw new IllegalArgumentException("First name is required");
    String s=normalizeStatus(r.status());
    if(!Set.of("ACTIVE","INACTIVE","ON_LEAVE","ARCHIVED").contains(s)) throw new IllegalArgumentException("Unsupported teacher status");
    if(r.employeeCode()!=null && trim(r.employeeCode())==null) throw new IllegalArgumentException("Employee code cannot be blank");
  }
  private String normalizeStatus(String s){String v=trim(s);return v==null?"ACTIVE":v.toUpperCase();}
  private void validateClassroom(ClassroomRequest r){
    if(r==null || trim(r.name())==null) throw new IllegalArgumentException("Classroom name is required");
    if(r.capacity()!=null && r.capacity()<0) throw new IllegalArgumentException("Classroom capacity cannot be negative");
  }
  private String trim(String s){if(s==null)return null;String v=s.trim();return v.isEmpty()?null:v;}
  private int count(String sql,Object... args){Integer n=db.queryForObject(sql,Integer.class,args);return n==null?0:n;}
  private void requireTeacher(UUID t,UUID id){if(id==null||count("select count(*) from teachers where id=? and tenant_id=?",id,t)==0)throw new IllegalArgumentException("Teacher not found");}
  private void requireClassroom(UUID t,UUID id){if(id==null||count("select count(*) from classrooms where id=? and tenant_id=?",id,t)==0)throw new IllegalArgumentException("Classroom not found");}
  private Map<String,Object> teacherRow(UUID t,UUID id){
    return db.queryForMap("select id,user_id,employee_code,first_name,last_name,email,phone,status from teachers where id=? and tenant_id=?",id,t);
  }
  private Map<String,Object> classroomRow(UUID t,UUID id){
    return db.queryForMap("select id,name,room_code,capacity,active from classrooms where id=? and tenant_id=?",id,t);
  }

  public record TeacherRequest(UUID userId,String employeeCode,String firstName,String lastName,String email,String phone,String status){}
  public record AssignmentRequest(List<UUID> batchIds,List<UUID> subjectIds){}
  public record ClassroomRequest(String name,String roomCode,Integer capacity,Boolean active){}
}
