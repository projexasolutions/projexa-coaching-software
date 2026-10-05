package com.projexa.coaching.notifications.controller;
import com.projexa.coaching.common.tenant.TenantContextHolder; import org.springframework.jdbc.core.JdbcTemplate; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.security.core.Authentication; import org.springframework.web.bind.annotation.*; import java.time.LocalDateTime; import java.util.*;
@RestController @RequestMapping("/api/v1/notifications") public class NotificationController {private final JdbcTemplate db;public NotificationController(JdbcTemplate db){this.db=db;}
 @GetMapping public List<Map<String,Object>> list(@RequestParam(defaultValue="false") boolean unreadOnly,Authentication auth){
 UUID t=TenantContextHolder.getRequired(); UUID u=UUID.fromString(auth.getName());
 return db.queryForList("select * from notifications where tenant_id=? and user_id=? and (?=false or read_at is null) order by created_at desc limit 100",t,u,!unreadOnly);
}
@GetMapping("/unread-count") public Map<String,Object> unreadCount(Authentication auth){
 UUID t=TenantContextHolder.getRequired(); UUID u=UUID.fromString(auth.getName());
 Integer n=db.queryForObject("select count(*) from notifications where tenant_id=? and user_id=? and read_at is null",Integer.class,t,u);
 return Map.of("count",n==null?0:n);
}
 @PostMapping("/{id}/read") public Map<String,Object> read(@PathVariable UUID id, Authentication auth){int n=db.update("update notifications set read_at=? where id=? and tenant_id=? and user_id=?",LocalDateTime.now(),id,TenantContextHolder.getRequired(),UUID.fromString(auth.getName()));return Map.of("updated",n>0);}
 @PostMapping("/broadcast") @PreAuthorize("hasAuthority('communication.send')") public Map<String,Object> broadcast(@RequestBody Broadcast req){
 if(req==null||req.userIds()==null||req.userIds().isEmpty()) throw new IllegalArgumentException("At least one notification recipient is required");
 if(req.title()==null||req.title().isBlank()) throw new IllegalArgumentException("Notification title is required");
 if(req.body()==null||req.body().isBlank()) throw new IllegalArgumentException("Notification body is required");
 if(req.userIds().stream().anyMatch(Objects::isNull)||new HashSet<>(req.userIds()).size()!=req.userIds().size()) throw new IllegalArgumentException("Duplicate or empty notification recipients are not allowed");
 UUID t=TenantContextHolder.getRequired();int n=0;for(UUID u:req.userIds()){Integer ok=db.queryForObject("select count(*) from users where id=? and tenant_id=? and status='ACTIVE'",Integer.class,u,t); if(ok==null||ok==0) throw new IllegalArgumentException("Notification recipient is not in this institute"); db.update("insert into notifications(id,tenant_id,user_id,type,title,body) values(?,?,?,?,?,?)",UUID.randomUUID(),t,u,req.type(),req.title(),req.body());n++;}return Map.of("queued",n);}
 public record Broadcast(List<UUID> userIds,String type,String title,String body){}
}
