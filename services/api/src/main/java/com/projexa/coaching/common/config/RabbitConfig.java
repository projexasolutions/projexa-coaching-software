package com.projexa.coaching.common.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
  public static final String EXCHANGE = "projexa.events";
  public static final String AUTOMATION_QUEUE = "projexa.automation";

  @Bean TopicExchange domainExchange() { return new TopicExchange(EXCHANGE, true, false); }
  @Bean Queue automationQueue() { return QueueBuilder.durable(AUTOMATION_QUEUE).build(); }
  @Bean Binding automationBinding(Queue automationQueue, TopicExchange domainExchange) { return BindingBuilder.bind(automationQueue).to(domainExchange).with("#"); }
  @Bean Jackson2JsonMessageConverter rabbitJsonConverter() { return new Jackson2JsonMessageConverter(); }
}
