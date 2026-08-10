package com.sms.chatApp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    private String id;
    private String conversationId;
    private String role;
    private String senderId;
    private String message;
    private long timestamp;
    private String status;

    
}