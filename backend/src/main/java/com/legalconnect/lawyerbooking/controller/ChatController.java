package com.legalconnect.lawyerbooking.controller;

import com.legalconnect.lawyerbooking.dto.MessageDTO;
import com.legalconnect.lawyerbooking.dto.MessageRequest;
import com.legalconnect.lawyerbooking.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    @Autowired
    private MessageService messageService;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload MessageRequest request, java.security.Principal principal) {
        // The principal here is our UserPrincipal set in WebSocketConfig
        // However, MessageService now uses SecurityContextHolder.
        // For STOMP, we might need to manually set the context if it's not automatically propagated.
        // But often SimpAnnotationMethodMessageHandler handles this.
        
        // 1. Persist and broadcast the message using service logic
        messageService.sendMessage(request);
    }
}
