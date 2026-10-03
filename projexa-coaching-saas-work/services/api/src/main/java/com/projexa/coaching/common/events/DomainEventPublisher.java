package com.projexa.coaching.common.events;
public interface DomainEventPublisher { void publish(DomainEvent event); }