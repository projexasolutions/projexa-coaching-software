package com.projexa.coaching.communication.service;
import org.springframework.stereotype.Component; import java.util.UUID;
@Component public class LocalVideoProvider implements VideoProvider { public String createRoom(String title){return "projexa-"+UUID.randomUUID();} public void endRoom(String externalRoomId){} }