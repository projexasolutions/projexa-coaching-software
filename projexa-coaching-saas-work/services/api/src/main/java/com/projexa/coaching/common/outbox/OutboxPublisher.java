package com.projexa.coaching.common.outbox;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projexa.coaching.common.config.RabbitConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class OutboxPublisher {
  private final JdbcTemplate db;
  private final RabbitTemplate rabbit;
  private final ObjectMapper mapper;
  public OutboxPublisher(JdbcTemplate db, RabbitTemplate rabbit, ObjectMapper mapper){this.db=db;this.rabbit=rabbit;this.mapper=mapper;}

  @Scheduled(fixedDelayString="${outbox.poll-ms:2000}")
  public void publishPending(){
    List<Map<String,Object>> events=db.queryForList("select id,tenant_id,aggregate_type,aggregate_id,event_type,payload from outbox_events where status='PENDING' and available_at<=now() order by created_at limit 50");
    for(Map<String,Object> e:events){
      UUID id=(UUID)e.get("id");
      int claimed=db.update("update outbox_events set status='PUBLISHING',attempts=attempts+1 where id=? and status='PENDING'",id);
      if(claimed!=1) continue;
      try{
        Map<String,Object> message=new LinkedHashMap<>(); message.put("id",id.toString()); message.put("tenantId",e.get("tenant_id")); message.put("aggregateType",e.get("aggregate_type")); message.put("aggregateId",e.get("aggregate_id")); message.put("eventType",e.get("event_type"));
        Object payload=e.get("payload"); message.put("payload", payload instanceof String ? mapper.readValue((String)payload,new TypeReference<Map<String,Object>>() {}) : payload);
        rabbit.convertAndSend(RabbitConfig.EXCHANGE,String.valueOf(e.get("event_type")),message);
        db.update("update outbox_events set status='PUBLISHED',published_at=now() where id=?",id);
      }catch(Exception ex){db.update("update outbox_events set status=case when attempts>=5 then 'FAILED' else 'PENDING' end,available_at=now()+interval '30 seconds' where id=?",id);}
    }
  }
}
