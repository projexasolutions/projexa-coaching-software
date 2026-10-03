package com.projexa.coaching.admissions.controller;
import com.projexa.coaching.common.tenant.TenantContextHolder; import org.springframework.jdbc.core.JdbcTemplate; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/v1/admissions") public class AdmissionsController {private final JdbcTemplate db;public AdmissionsController(JdbcTemplate db){this.db=db;}
@GetMapping("/leads") @PreAuthorize("hasAuthority('admissions.manage')") public List<Map<String,Object>> list(){return db.queryForList("select * from leads where tenant_id=? order by updated_at desc",TenantContextHolder.getRequired());}
@PostMapping("/leads") @PreAuthorize("hasAuthority('admissions.manage')") public Map<String,Object> create(@RequestBody Lead req){UUID t=TenantContextHolder.getRequired(),id=UUID.randomUUID();db.update("insert into leads(id,tenant_id,name,phone,email,source,stage) values(?,?,?,?,?,?,?)",id,t,req.name(),req.phone(),req.email(),req.source(),req.stage()==null?"ENQUIRY":req.stage());return Map.of("id",id);}
@PostMapping("/leads/{id}/stage") @PreAuthorize("hasAuthority('admissions.manage')") public Map<String,Object> move(@PathVariable UUID id,@RequestBody Map<String,String> req){int n=db.update("update leads set stage=?,updated_at=now() where id=? and tenant_id=?",req.get("stage"),id,TenantContextHolder.getRequired());return Map.of("updated",n>0);}
public record Lead(String name,String phone,String email,String source,String stage){}
}
