package com.projexa.coaching.communication.service;
public interface VideoProvider { String createRoom(String title); void endRoom(String externalRoomId); }