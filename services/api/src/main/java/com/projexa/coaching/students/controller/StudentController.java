package com.projexa.coaching.students.controller;

import com.projexa.coaching.students.entity.Student;
import com.projexa.coaching.students.service.StudentService;
import com.projexa.coaching.common.responses.ApiResponse;
import com.projexa.coaching.common.tenant.TenantContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/students")
public class StudentController {
 private final StudentService service; private final JdbcTemplate db;
 public StudentController(StudentService service,JdbcTemplate db){this.service=service;this.db=db;}

 @GetMapping
 @PreAuthorize("hasAuthority('dashboard.read') or hasAuthority('settings.manage') or hasAuthority('students.read')")
 public ApiResponse<List<Student>> list(){return ApiResponse.ok(service.list());}

 @GetMapping("/operational")
 @PreAuthorize("hasAuthority('dashboard.read') or hasAuthority('students.read') or hasAuthority('settings.manage')")
 public List<Map<String,Object>> operational(){
   UUID t=TenantContextHolder.getRequired();
   return db.queryForList("select s.*,e.id enrollment_id,e.batch_id,e.class_id,e.stream_id,e.academic_year_id,b.name batch_name,c.name class_name,st.name stream_name,ay.name academic_year from students s left join enrollments e on e.student_id=s.id and e.tenant_id=s.tenant_id and e.status='ACTIVE' left join batches b on b.id=e.batch_id left join classes c on c.id=e.class_id left join streams st on st.id=e.stream_id left join academic_years ay on ay.id=e.academic_year_id where s.tenant_id=? order by s.created_at desc",t);
 }

 @GetMapping("/{id}")
 @PreAuthorize("hasAuthority('students.read') or hasAnyRole('INSTITUTE_OWNER','INSTITUTE_ADMIN')")
 public ApiResponse<Student> get(@PathVariable UUID id){return ApiResponse.ok(service.get(id));}

 @PostMapping
 @PreAuthorize("hasAuthority('settings.manage') or hasAuthority('students.create')")
 public ApiResponse<Student> create(@RequestBody Student item){return ApiResponse.ok(service.create(item));}

 @PutMapping("/{id}")
 @PreAuthorize("hasAuthority('settings.manage') or hasAuthority('students.update')")
 public ApiResponse<Student> update(@PathVariable UUID id,@RequestBody Student item){return ApiResponse.ok(service.update(id,item));}

 @PostMapping("/{id}/enrollment")
 @PreAuthorize("hasAuthority('settings.manage') or hasAuthority('students.update')")
 public Map<String,Object> enroll(@PathVariable UUID id,@RequestBody EnrollmentRequest req){
   UUID t=TenantContextHolder.getRequired();
   db.queryForObject("select id from students where id=? and tenant_id=?",UUID.class,id,t);
   db.queryForObject("select id from batches where id=? and tenant_id=?",UUID.class,req.batchId(),t);
   db.update("update enrollments set status='INACTIVE' where student_id=? and tenant_id=? and academic_year_id=? and status='ACTIVE'",id,t,req.academicYearId());
   UUID enrollment=UUID.randomUUID();
   db.update("insert into enrollments(id,tenant_id,student_id,academic_year_id,class_id,stream_id,batch_id,status) values(?,?,?,?,?,?,?,'ACTIVE') on conflict(tenant_id,student_id,academic_year_id) do update set class_id=excluded.class_id,stream_id=excluded.stream_id,batch_id=excluded.batch_id,status='ACTIVE'",enrollment,t,id,req.academicYearId(),req.classId(),req.streamId(),req.batchId());
   return db.queryForMap("select e.*,b.name batch_name,c.name class_name,st.name stream_name,ay.name academic_year from enrollments e join batches b on b.id=e.batch_id join classes c on c.id=e.class_id left join streams st on st.id=e.stream_id join academic_years ay on ay.id=e.academic_year_id where e.tenant_id=? and e.student_id=? and e.academic_year_id=?",t,id,req.academicYearId());
 }

 @DeleteMapping("/{id}")
 @PreAuthorize("hasAuthority('settings.manage') or hasAuthority('students.delete')")
 public ApiResponse<Void> delete(@PathVariable UUID id){service.delete(id);return ApiResponse.ok(null);}

 public record EnrollmentRequest(UUID academicYearId,UUID classId,UUID streamId,UUID batchId){}
}
