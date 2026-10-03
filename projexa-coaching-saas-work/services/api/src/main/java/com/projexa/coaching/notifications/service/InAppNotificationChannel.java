package com.projexa.coaching.notifications.service;
import org.springframework.stereotype.Component;
@Component public class InAppNotificationChannel implements NotificationChannel { public String type(){return "IN_APP";} public void send(String recipient,String title,String body){ /* persisted notification delivery is implemented by the notification service */ } }