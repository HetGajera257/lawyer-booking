package com.legalconnect.lawyerbooking.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import com.legalconnect.lawyerbooking.service.MessageService;
import com.legalconnect.lawyerbooking.service.AuthorizationService;
import com.legalconnect.lawyerbooking.util.JwtUtil;
import com.legalconnect.lawyerbooking.dto.MessageDTO;
import com.legalconnect.lawyerbooking.dto.MessageRequest;
import com.legalconnect.lawyerbooking.exception.UnauthorizedException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = "*")
public class MessageController {

    private static final Logger logger = LoggerFactory.getLogger(MessageController.class);

    @Autowired
    private MessageService messageService;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/send")
    public ResponseEntity<MessageDTO> sendMessage(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody MessageRequest request) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new UnauthorizedException("Authorization token required");
            }
            
            String token = authHeader.substring(7);
            Long userId = jwtUtil.extractUserId(token);
            String userType = jwtUtil.extractUserType(token);
            
            // Verify sender matches authenticated user
            if (!request.getSenderId().equals(userId) || 
                !request.getSenderType().equals(userType)) {
                throw new UnauthorizedException("Sender information does not match authenticated user");
            }
            
            MessageDTO messageDTO = messageService.sendMessage(request);
            logger.info("Message sent successfully from {} {} to {} {}", 
                       request.getSenderType(), request.getSenderId(),
                       request.getReceiverType(), request.getReceiverId());
            return ResponseEntity.ok(messageDTO);
        } catch (UnauthorizedException e) {
            logger.warn("Unauthorized message send attempt: {}", e.getMessage());
            return ResponseEntity.status(401).build();
        } catch (Exception e) {
            logger.error("Error sending message", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/case/{caseId}")
    public ResponseEntity<List<MessageDTO>> getMessagesByCaseId(@PathVariable Long caseId) {
        List<MessageDTO> messages = messageService.getMessagesByCaseId(caseId);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/receiver/{receiverId}/{receiverType}")
    public ResponseEntity<List<MessageDTO>> getMessagesByReceiver(
            @PathVariable Long receiverId,
            @PathVariable String receiverType) {
        List<MessageDTO> messages = messageService.getMessagesByReceiver(receiverId, receiverType);
        return ResponseEntity.ok(messages);
    }

    @PutMapping("/{messageId}/read")
    public ResponseEntity<Void> markMessageAsRead(@PathVariable Long messageId) {
        try {
            messageService.markMessageAsRead(messageId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/unread-count/{receiverId}/{receiverType}")
    public ResponseEntity<Map<String, Long>> getUnreadMessageCount(
            @PathVariable Long receiverId,
            @PathVariable String receiverType) {
        long count = messageService.getUnreadMessageCount(receiverId, receiverType);
        return ResponseEntity.ok(Map.of("count", count));
    }
}

