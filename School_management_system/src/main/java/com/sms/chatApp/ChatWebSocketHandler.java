package com.sms.chatApp;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {

        sessions.add(session);

        System.out.println(
            "Connected: " + session.getId()
        );
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

    	System.out.println("Received from " + session.getId() + ": " + message.getPayload());
        // Convert JSON to ChatMessage object
        ChatMessage chatMessage = objectMapper.readValue(
                message.getPayload(),
                ChatMessage.class
        );

        // Convert object back to JSON
        String json = objectMapper.writeValueAsString(chatMessage);

        // Broadcast to all connected clients
        for (WebSocketSession client : sessions) {
            if (client.isOpen()) {
                client.sendMessage(new TextMessage(json));
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        System.out.println("Disconnected: " + session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        sessions.remove(session);
        System.err.println("Error: " + exception.getMessage());
    }
    
}