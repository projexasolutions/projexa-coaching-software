package com.projexa.coaching.automation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.time.*; import java.util.*;

@Service
public class AutomationExecutionService {
 private final JdbcTemplate db; private final ObjectMapper mapper;
 public AutomationExecutionService(JdbcTemplate db,ObjectMapper mapper){this.db=db;this.mapper=mapper;}

 @RabbitListener(queues="projexa.automation")
 public void consume(Map<String,Object> event){
   UUID tenant=UUID.fromString(String.valueOf(event.get("tenantId")));
   String type=String.valueOf(event.get("eventType")); String id=String.valueOf(event.get("id"));
   executeEvent(tenant,type,id,event.get("payload"));
 }

 public void executeEvent(UUID tenant,String eventType,String eventId,Object payload){
   List<Map<String,Object>> rules=db.queryForList("select id,name,cooldown_seconds,max_depth from automation_rules where tenant_id=? and active=true and trigger_event=?",tenant,eventType);
   for(Map<String,Object> rule:rules){
     UUID ruleId=(UUID)rule.get("id"); if(cooldownActive(ruleId))continue;
     List<Map<String,Object>> conditions=db.queryForList("select field_name,operator,comparison_value from automation_conditions where rule_id=? order by group_no",ruleId);
     if(!matches(conditions,payload))continue;
     Integer duplicate=db.queryForObject("select count(*) from automation_runs where rule_id=? and event_id=?",Integer.class,ruleId,eventId); if(duplicate!=null&&duplicate>0)continue;
     UUID run=UUID.randomUUID();
     try{
       db.update("insert into automation_runs(id,tenant_id,rule_id,event_id,status,attempt_count,started_at) values(?,?,?,?,?,?,now())",run,tenant,ruleId,eventId,"RUNNING",1);
       List<Map<String,Object>> actions=db.queryForList("select action_type,action_config from automation_actions where rule_id=? order by action_order",ruleId);
       for(Map<String,Object> a:actions)perform(tenant,String.valueOf(a.get("action_type")),a.get("action_config"),payload);
       db.update("update automation_runs set status='SUCCESS',completed_at=now() where id=?",run);
     }catch(Exception ex){db.update("update automation_runs set status='FAILED',error_message=?,completed_at=now() where id=?",ex.getMessage(),run);}
   }
 }

 private boolean cooldownActive(UUID rule){Integer seconds=db.queryForObject("select coalesce(cooldown_seconds,0) from automation_rules where id=?",Integer.class,rule);if(seconds==null||seconds<=0)return false;Integer count=db.queryForObject("select count(*) from automation_runs where rule_id=? and status='SUCCESS' and started_at>?",Integer.class,rule,LocalDateTime.now().minusSeconds(seconds));return count!=null&&count>0;}
 private boolean matches(List<Map<String,Object>> cs,Object payload){if(cs.isEmpty())return true;Map<?,?> map=payload instanceof Map<?,?> m?m:Map.of("value",String.valueOf(payload));for(Map<String,Object> c:cs){Object actual=map.get(String.valueOf(c.get("field_name")));String op=String.valueOf(c.get("operator"));String expected=String.valueOf(c.get("comparison_value"));boolean ok=switch(op){case "EQUALS"->Objects.equals(String.valueOf(actual),expected);case "NOT_EQUALS"->!Objects.equals(String.valueOf(actual),expected);case "CONTAINS"->actual!=null&&String.valueOf(actual).contains(expected);case "NOT_CONTAINS"->actual==null||!String.valueOf(actual).contains(expected);case "IS_EMPTY"->actual==null||String.valueOf(actual).isBlank();case "IS_NOT_EMPTY"->actual!=null&&!String.valueOf(actual).isBlank();case "GREATER_THAN"->num(actual)>Double.parseDouble(expected);case "LESS_THAN"->num(actual)<Double.parseDouble(expected);case "GREATER_THAN_OR_EQUAL"->num(actual)>=Double.parseDouble(expected);case "LESS_THAN_OR_EQUAL"->num(actual)<=Double.parseDouble(expected);case "IN"->Arrays.asList(expected.split(",")).contains(String.valueOf(actual));case "NOT_IN"->!Arrays.asList(expected.split(",")).contains(String.valueOf(actual));case "BETWEEN"->{String[] x=expected.split(",",2);double n=num(actual);yield x.length==2&&n>=Double.parseDouble(x[0])&&n<=Double.parseDouble(x[1]);}default->false;};if(!ok)return false;}return true;}
 private double num(Object o){try{return Double.parseDouble(String.valueOf(o));}catch(Exception e){return Double.NaN;}}
 private void perform(UUID tenant,String type,Object raw,Object payload){
   Map<String,Object> cfg=new HashMap<>();try{if(raw!=null)cfg=mapper.readValue(String.valueOf(raw),Map.class);}catch(Exception ignored){}
   UUID user=uuid(cfg.get("userId")); UUID student=uuid(cfg.get("studentId")); UUID lead=uuid(cfg.get("leadId")); UUID ticket=uuid(cfg.get("ticketId")); UUID assignee=uuid(cfg.get("assignedUserId"));
   String title=String.valueOf(cfg.getOrDefault("title","Automation")); String body=String.valueOf(cfg.getOrDefault("body",String.valueOf(payload)));
   if(type.startsWith("SEND_")||type.equals("CREATE_ALERT")||type.equals("GENERATE_REPORT")||type.equals("SEND_REPORT")){
     if(user==null)user=studentUser(tenant,student); if(user!=null)db.update("insert into notifications(id,tenant_id,user_id,type,title,body) values(?,?,?,?,?,?)",UUID.randomUUID(),tenant,user,type,title,body); return;
   }
   if(type.equals("ADD_TO_WATCHLIST")&&student!=null){db.update("insert into student_watchlist(id,tenant_id,student_id,reason,active) values(?,?,?,?,true) on conflict(tenant_id,student_id) do update set reason=excluded.reason,active=true",UUID.randomUUID(),tenant,student,body);return;}
   if(type.equals("APPLY_ATTENDANCE_RESTRICTION")&&student!=null){db.update("insert into student_attendance_restrictions(id,tenant_id,student_id,reason,active) values(?,?,?,?,true) on conflict(tenant_id,student_id) do update set reason=excluded.reason,active=true",UUID.randomUUID(),tenant,student,body);return;}
   if(type.equals("REMOVE_ATTENDANCE_RESTRICTION")&&student!=null){db.update("update student_attendance_restrictions set active=false where tenant_id=? and student_id=?",tenant,student);return;}
   if(type.equals("CREATE_FOLLOW_UP")&&lead!=null){db.update("insert into follow_ups(id,tenant_id,lead_id,assigned_user_id,due_at,status,notes) values(?,?,?,?,?,'PENDING',?)",UUID.randomUUID(),tenant,lead,assignee,LocalDateTime.now().plusDays(1),body);return;}
   if(type.equals("ASSIGN_LEAD")&&lead!=null&&assignee!=null){db.update("update leads set counsellor_user_id=?,updated_at=now() where id=? and tenant_id=?",assignee,lead,tenant);return;}
   if(type.equals("MOVE_PIPELINE_STAGE")&&lead!=null){db.update("update leads set stage=?,updated_at=now() where id=? and tenant_id=?",String.valueOf(cfg.getOrDefault("stage","FOLLOW_UP")),lead,tenant);return;}
   if(type.equals("CREATE_TICKET")){db.update("insert into tickets(id,tenant_id,created_by,assigned_to,subject,description,priority,status) values(?,?,?,?,?,?,?,?)",UUID.randomUUID(),tenant,user,assignee,title,body,String.valueOf(cfg.getOrDefault("priority","NORMAL")),"OPEN");return;}
   if(type.equals("ASSIGN_TICKET")&&ticket!=null&&assignee!=null){db.update("update tickets set assigned_to=?,updated_at=now() where id=? and tenant_id=?",assignee,ticket,tenant);return;}
   if(type.equals("ESCALATE_TICKET")&&ticket!=null){db.update("update tickets set priority='HIGH',status='ESCALATED',updated_at=now() where id=? and tenant_id=?",ticket,tenant);return;}
   if(type.equals("ASSIGN_TASK")){db.update("insert into staff_tasks(id,tenant_id,assigned_to,student_id,lead_id,title,due_at,status) values(?,?,?,?,?,?,?,'OPEN')",UUID.randomUUID(),tenant,assignee,student,lead,title,LocalDateTime.now().plusDays(1));}
 }
 private UUID uuid(Object x){try{return x==null?null:UUID.fromString(String.valueOf(x));}catch(Exception e){return null;}}
 private UUID studentUser(UUID tenant,UUID student){if(student==null)return null;try{return db.queryForObject("select user_id from students where id=? and tenant_id=?",UUID.class,student,tenant);}catch(Exception e){return null;}}
}
