package com.projexa.coaching.common.events;
import java.util.UUID;
public record DomainEvent(String eventId, UUID tenantId, String eventType, UUID aggregateId, Object payload) {}