package com.legalconnect.lawyerbooking.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import com.legalconnect.lawyerbooking.service.MessageService;
import com.legalconnect.lawyerbooking.service.AuthorizationService;
import com.legalconnect.lawyerbooking.dto.MessageDTO;
import com.legalconnect.lawyerbooking.dto.MessageRequest;
import com.legalconnect.lawyerbooking.exception.UnauthorizedException;

import java.util.List;
import java.util.Map;

/**
 * SECURE MESSAGE CONTROLLER
 * 
 * PURPOSE: Handle case-based messaging with strict security
 * 
 * SECURITY FEATURES:
 * - All endpoints require authentication
 * - Sender identity derived from JWT (never from request)
 * - Case-based authorization enforced
 * - Role-based access control
 * - No identity spoofing possible
 */
@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = "*")
public class MessageController {

    private static final Logger logger = LoggerFactory.getLogger(MessageController.class);

    @Autowired
    private MessageService messageService;

    @Autowired
    private AuthorizationService authorizationService;

    /**
     * Send a message to a case participant
     * 
     * SECURITY: Sender info extracted from JWT, receiver validated
     * 
     * @param request MessageRequest with caseId, receiverId, receiverType, messageText
     * @return Created message DTO
     */
    @PostMapping("/send")
    @PreAuthorize("hasAnyRole('USER', 'LAWYER')")
    public ResponseEntity<MessageDTO> sendMessage(@RequestBody MessageRequest request) {
        try {
            logger.info("Message send request for case {} from authenticated user", request.getCaseId());
            
            // Service will verify access using SecurityContext and extract sender identity
            MessageDTO messageDTO = messageService.sendMessage(request);
            
            logger.info("Message sent successfully: ID={}, Case={}, Sender={}/{}", 
                       messageDTO.getId(), messageDTO.getCaseId(), 
                       messageDTO.getSenderId(), messageDTO.getSenderType());
            
            return ResponseEntity.ok(messageDTO);
            
        } catch (UnauthorizedException e) {
            logger.warn("Unauthorized message send attempt: {}", e.getMessage());
            return ResponseEntity.status(401).build();
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid message request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error sending message", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get all messages for a specific case
     * 
     * SECURITY: Only case owner or assigned lawyer can access
     * 
     * @param caseId Case ID
     * @return List of message DTOs ordered by creation time
     */
    @GetMapping("/case/{caseId}")
    @PreAuthorize("hasAnyRole('USER', 'LAWYER')")
    public ResponseEntity<List<MessageDTO>> getMessagesByCaseId(@PathVariable Long caseId) {
        try {
            logger.info("Fetching messages for case {} by authenticated user", caseId);
            
            // Verify user has access to this case
            authorizationService.verifyMessageAccess(caseId);
            
            List<MessageDTO> messages = messageService.getMessagesByCaseId(caseId);
            
            logger.info("Retrieved {} messages for case {}", messages.size(), caseId);
            return ResponseEntity.ok(messages);
            
        } catch (UnauthorizedException e) {
            logger.warn("Unauthorized access to case {} messages: {}", caseId, e.getMessage());
            return ResponseEntity.status(401).build();
        } catch (Exception e) {
            logger.error("Error fetching messages for case {}", caseId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Mark a message as read
     * 
     * SECURITY: Only receiver can mark message as read
     * 
     * @param messageId Message ID
     * @return 200 if successful
     */
    @PutMapping("/{messageId}/read")
    @PreAuthorize("hasAnyRole('USER', 'LAWYER')")
    public ResponseEntity<Void> markMessageAsRead(@PathVariable Long messageId) {
        try {
            logger.info("Marking message {} as read by authenticated user", messageId);
            
            messageService.markMessageAsRead(messageId);
            
            logger.info("Message {} marked as read successfully", messageId);
            return ResponseEntity.ok().build();
            
        } catch (UnauthorizedException e) {
            logger.warn("Unauthorized attempt to mark message {} as read: {}", messageId, e.getMessage());
            return ResponseEntity.status(401).build();
        } catch (RuntimeException e) {
            logger.warn("Message {} not found or access denied: {}", messageId, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error marking message {} as read", messageId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get unread message count for current user in a specific case
     * 
     * SECURITY: Only case owner or assigned lawyer can access
     * 
     * @param caseId Case ID
     * @return Unread message count
     */
    @GetMapping("/case/{caseId}/unread-count")
    @PreAuthorize("hasAnyRole('USER', 'LAWYER')")
    public ResponseEntity<Map<String, Long>> getUnreadMessageCountForCase(@PathVariable Long caseId) {
        try {
            logger.info("Getting unread count for case {} by authenticated user", caseId);
            
            // Verify user has access to this case
            authorizationService.verifyMessageAccess(caseId);
            
            long count = messageService.getUnreadMessageCountForCase(caseId);
            
            logger.info("Unread count for case {}: {}", caseId, count);
            return ResponseEntity.ok(Map.of("count", count));
            
        } catch (UnauthorizedException e) {
            logger.warn("Unauthorized access to unread count for case {}: {}", caseId, e.getMessage());
            return ResponseEntity.status(401).build();
        } catch (Exception e) {
            logger.error("Error getting unread count for case {}", caseId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * DEPRECATED: Get messages by receiver (removed for security)
     * 
     * This endpoint is removed because it bypasses case-based authorization.
     * Use /api/messages/case/{caseId} instead.
     */
    @Deprecated
    @GetMapping("/receiver/{receiverId}/{receiverType}")
    public ResponseEntity<List<MessageDTO>> getMessagesByReceiver(
            @PathVariable Long receiverId,
            @PathVariable String receiverType) {
        logger.warn("Deprecated endpoint called: /receiver/{}/{}/ - use case-based endpoint instead", receiverId, receiverType);
        return ResponseEntity.badRequest().build();
    }

    /**
     * DEPRECATED: Get unread count by receiver (removed for security)
     * 
     * This endpoint is removed because it bypasses case-based authorization.
     * Use /api/messages/case/{caseId}/unread-count instead.
     */
    @Deprecated
    @GetMapping("/unread-count/{receiverId}/{receiverType}")
    public ResponseEntity<Map<String, Long>> getUnreadMessageCount(
            @PathVariable Long receiverId,
            @PathVariable String receiverType) {
        logger.warn("Deprecated endpoint called: /unread-count/{}/{}/ - use case-based endpoint instead", receiverId, receiverType);
        return ResponseEntity.badRequest().build();
    }
}

