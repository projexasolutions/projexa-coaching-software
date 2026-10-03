package com.projexa.coaching.notifications.service;
public interface NotificationChannel { String type(); void send(String recipient,String title,String body); }