package com.projexa.coaching.support.controller;
import com.projexa.coaching.common.tenant.TenantContextHolder; import org.springframework.jdbc.core.JdbcTemplate; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.security.core.Authentication; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/v1/support") public class SupportController {private final JdbcTemplate db;public SupportController(JdbcTemplate db){this.db=db;}
@GetMapping("/tickets") public List<Map<String,Object>> list(){return db.queryForList("select * from tickets where tenant_id=? order by created_at desc",TenantContextHolder.getRequired());}
@PostMapping("/tickets") public Map<String,Object> create(@RequestBody Ticket req, Authentication auth){UUID t=TenantContextHolder.getRequired(),id=UUID.randomUUID(); UUID creator=UUID.fromString(auth.getName()); db.update("insert into tickets(id,tenant_id,created_by,subject,description,priority,status) values(?,?,?,?,?,?,?)",id,t,creator,req.subject(),req.description(),req.priority()==null?"NORMAL":req.priority(),"OPEN");return Map.of("id",id);}
@PostMapping("/tickets/{id}/status") @PreAuthorize("hasAuthority('support.manage')") public Map<String,Object> status(@PathVariable UUID id,@RequestBody Map<String,String> body){int n=db.update("update tickets set status=?,updated_at=now() where id=? and tenant_id=?",body.get("status"),id,TenantContextHolder.getRequired());return Map.of("updated",n>0);}
public record Ticket(String subject,String description,String priority){}
}
