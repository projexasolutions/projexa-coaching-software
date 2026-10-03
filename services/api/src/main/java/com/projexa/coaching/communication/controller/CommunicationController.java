package com.projexa.coaching.communication.controller;

import com.projexa.coaching.common.tenant.TenantContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/communication")
public class CommunicationController {
  private final JdbcTemplate db;
  public CommunicationController(JdbcTemplate db){this.db=db;}

  @PostMapping("/conversations")
  @PreAuthorize("hasAuthority('communication.send')")
  public Map<String,Object> create(@RequestBody CreateConversation req, Authentication auth){
    UUID t=TenantContextHolder.getRequired(), caller=UUID.fromString(auth.getName());
    if(req.userIds()==null||req.userIds().isEmpty()) throw new IllegalArgumentException("At least one participant is required");
    if(req.type()==null||!Set.of("DIRECT","GROUP","ANNOUNCEMENT").contains(req.type().toUpperCase())) throw new IllegalArgumentException("Invalid conversation type");
    List<UUID> participants=new ArrayList<>(new LinkedHashSet<>(req.userIds())); if(!participants.contains(caller)) participants.add(caller);
    for(UUID u:participants) requireActiveUser(t,u);
    UUID c=UUID.randomUUID();
    db.update("insert into conversations(id,tenant_id,type,title) values(?,?,?,?)",c,t,req.type().toUpperCase(),clean(req.title()));
    for(UUID u:participants) db.update("insert into conversation_members(conversation_id,user_id) values(?,?) on conflict do nothing",c,u);
    return detail(t,c,caller);
  }

  @GetMapping("/conversations")
  public List<Map<String,Object>> list(Authentication auth){
    UUID t=TenantContextHolder.getRequired(),u=UUID.fromString(auth.getName());
    return db.queryForList("""
      select c.*, (select max(m.created_at) from messages m where m.conversation_id=c.id and m.tenant_id=c.tenant_id) last_message_at,
      (select count(*) from conversation_members x where x.conversation_id=c.id) member_count
      from conversations c join conversation_members cm on cm.conversation_id=c.id
      where c.tenant_id=? and cm.user_id=? order by coalesce((select max(m.created_at) from messages m where m.conversation_id=c.id),c.created_at) desc
      """,t,u);
  }

  @GetMapping("/conversations/{id}")
  public Map<String,Object> detail(@PathVariable UUID id,Authentication auth){
    return detail(TenantContextHolder.getRequired(),id,UUID.fromString(auth.getName()));
  }

  @GetMapping("/conversations/{id}/messages")
  public List<Map<String,Object>> messages(@PathVariable UUID id,Authentication auth){
    UUID t=TenantContextHolder.getRequired(),u=UUID.fromString(auth.getName()); requireMember(t,id,u);
    return db.queryForList("select m.*,u.email sender_email from messages m join users u on u.id=m.sender_user_id where m.tenant_id=? and m.conversation_id=? order by m.created_at asc",t,id);
  }

  @PostMapping("/conversations/{id}/messages")
  public Map<String,Object> send(@PathVariable UUID id,@RequestBody SendMessage req,Authentication auth){
    UUID t=TenantContextHolder.getRequired(),sender=UUID.fromString(auth.getName()); requireMember(t,id,sender);
    if(req.body()==null||req.body().trim().isEmpty()) throw new IllegalArgumentException("Message cannot be empty");
    UUID mid=UUID.randomUUID(); LocalDateTime now=LocalDateTime.now();
    db.update("insert into messages(id,tenant_id,conversation_id,sender_user_id,body,created_at) values(?,?,?,?,?,?)",mid,t,id,sender,req.body().trim(),now);
    return Map.of("id",mid,"createdAt",now);
  }

  @PostMapping("/conversations/{id}/read")
  public Map<String,Object> read(@PathVariable UUID id,Authentication auth){
    UUID t=TenantContextHolder.getRequired(),u=UUID.fromString(auth.getName()); requireMember(t,id,u);
    int n=db.update("update messages set read_at=now() where tenant_id=? and conversation_id=? and sender_user_id<>? and read_at is null",t,id,u);
    return Map.of("updated",n);
  }

  private Map<String,Object> detail(UUID t,UUID id,UUID user){
    requireMember(t,id,user);
    return Map.of("conversation",db.queryForMap("select * from conversations where id=? and tenant_id=?",id,t),
      "members",db.queryForList("select u.id,u.email from users u join conversation_members cm on cm.user_id=u.id where cm.conversation_id=? order by u.email",id),
      "messages",db.queryForList("select m.*,u.email sender_email from messages m join users u on u.id=m.sender_user_id where m.tenant_id=? and m.conversation_id=? order by m.created_at asc",t,id));
  }
  private void requireMember(UUID t,UUID c,UUID u){if(count("select count(*) from conversation_members cm join conversations c on c.id=cm.conversation_id where cm.conversation_id=? and cm.user_id=? and c.tenant_id=?",c,u,t)==0) throw new org.springframework.security.access.AccessDeniedException("Conversation is not accessible");}
  private void requireActiveUser(UUID t,UUID u){if(count("select count(*) from users where id=? and tenant_id=? and status='ACTIVE'",u,t)==0) throw new org.springframework.security.access.AccessDeniedException("Participant is not in this institute");}
  private int count(String sql,Object... args){Integer n=db.queryForObject(sql,Integer.class,args);return n==null?0:n;}
  private String clean(String s){return s==null||s.isBlank()?null:s.trim();}
  public record CreateConversation(String type,String title,List<UUID> userIds){}
  public record SendMessage(String body){}
}