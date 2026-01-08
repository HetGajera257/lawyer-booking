package com.legalconnect.lawyerbooking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import com.legalconnect.lawyerbooking.entity.Message;
import com.legalconnect.lawyerbooking.entity.Case;
import com.legalconnect.lawyerbooking.exception.BadRequestException;
import com.legalconnect.lawyerbooking.exception.UnauthorizedException;
import com.legalconnect.lawyerbooking.repository.MessageRepository;
import com.legalconnect.lawyerbooking.repository.CaseRepository;
import com.legalconnect.lawyerbooking.dto.MessageDTO;
import com.legalconnect.lawyerbooking.dto.MessageRequest;
import com.legalconnect.lawyerbooking.security.UserPrincipal;

import java.util.List;
import java.util.stream.Collectors;

/**
 * SECURE MESSAGE SERVICE
 * 
 * PURPOSE: Handle case-based messaging with strict security enforcement
 * 
 * SECURITY FEATURES:
 * - Sender identity extracted from SecurityContext (never from request)
 * - Case-based authorization enforced
 * - Receiver validation against case participants
 * - Role-based access control
 * - No identity spoofing possible
 * 
 * CHAT ARCHITECTURE:
 * - Messages are case-scoped (not user-scoped)
 * - Only case owner (USER) and assigned lawyer can chat
 * - Messages broadcast to case-specific WebSocket topic
 * - Real-time delivery with fallback to REST
 */
@Service
@Transactional
public class MessageService {

    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private CaseRepository caseRepository;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    /**
     * Send a message to a case participant
     * 
     * SECURITY: Sender identity extracted from SecurityContext, receiver validated
     * 
     * @param request MessageRequest with caseId, receiverId, receiverType, messageText
     * @return Created message DTO
     */
    public MessageDTO sendMessage(MessageRequest request) {
        // Step 1: Extract authenticated user from SecurityContext
        UserPrincipal currentUser = getCurrentAuthenticatedUser();
        Long senderId = currentUser.getUserId();
        String senderType = currentUser.getUserType();

        // Step 2: Validate message text
        if (request.getMessageText() == null || request.getMessageText().trim().isEmpty()) {
            throw new BadRequestException("Message text cannot be empty");
        }

        // Step 3: Verify sender has access to the case
        authorizationService.verifyMessageAccess(request.getCaseId());

        // Step 4: Get case and validate receiver belongs to the same case
        Case caseEntity = getCaseAndValidateReceiver(request.getCaseId(), request.getReceiverId(), request.getReceiverType());

        // Step 5: Validate receiver is not the sender
        if (request.getReceiverId().equals(senderId) && request.getReceiverType().equalsIgnoreCase(senderType)) {
            throw new BadRequestException("Cannot send message to yourself");
        }

        logger.info("Sending message: Case={}, Sender={}/{}, Receiver={}/{}", 
                   request.getCaseId(), senderType, senderId, 
                   request.getReceiverType(), request.getReceiverId());

        // Step 6: Create and save message
        Message message = new Message();
        message.setCaseId(request.getCaseId());
        message.setSenderId(senderId);                    // From SecurityContext
        message.setSenderType(senderType);                // From SecurityContext
        message.setReceiverId(request.getReceiverId());    // From request
        message.setReceiverType(request.getReceiverType());// From request
        message.setMessageText(request.getMessageText().trim());
        message.setIsRead(false);

        Message savedMessage = messageRepository.save(message);
        MessageDTO messageDTO = convertToDTO(savedMessage);

        // Step 7: Broadcast to case-specific WebSocket topic
        broadcastMessageToCase(messageDTO);

        logger.info("Message sent successfully: ID={}, Case={}", savedMessage.getId(), savedMessage.getCaseId());
        return messageDTO;
    }

    /**
     * Get all messages for a specific case
     * 
     * SECURITY: Only case owner or assigned lawyer can access
     * 
     * @param caseId Case ID
     * @return List of message DTOs ordered by creation time
     */
    @Transactional(readOnly = true)
    public List<MessageDTO> getMessagesByCaseId(Long caseId) {
        // Verify access before fetching messages
        authorizationService.verifyMessageAccess(caseId);

        List<Message> messages = messageRepository.findByCaseIdOrderByCreatedAtAsc(caseId);
        
        logger.info("Retrieved {} messages for case {}", messages.size(), caseId);
        return messages.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    /**
     * Mark a message as read
     * 
     * SECURITY: Only the receiver can mark message as read
     * 
     * @param messageId Message ID
     */
    public void markMessageAsRead(Long messageId) {
        // Get current authenticated user
        UserPrincipal currentUser = getCurrentAuthenticatedUser();
        Long currentUserId = currentUser.getUserId();
        String currentUserType = currentUser.getUserType();

        // Find message
        Message message = messageRepository.findById(messageId)
            .orElseThrow(() -> new BadRequestException("Message not found with id: " + messageId));

        // Verify current user is the receiver
        if (!message.getReceiverId().equals(currentUserId) || 
            !message.getReceiverType().equalsIgnoreCase(currentUserType)) {
            throw new UnauthorizedException("Only the message receiver can mark it as read");
        }

        // Mark as read
        message.setIsRead(true);
        messageRepository.save(message);

        logger.info("Message {} marked as read by {}/{}", messageId, currentUserType, currentUserId);
    }

    /**
     * Get unread message count for current user in a specific case
     * 
     * SECURITY: Only case owner or assigned lawyer can access
     * 
     * @param caseId Case ID
     * @return Unread message count
     */
    @Transactional(readOnly = true)
    public long getUnreadMessageCountForCase(Long caseId) {
        // Verify access before counting
        authorizationService.verifyMessageAccess(caseId);

        // Get current user
        UserPrincipal currentUser = getCurrentAuthenticatedUser();
        Long currentUserId = currentUser.getUserId();
        String currentUserType = currentUser.getUserType();

        // Count unread messages for this user in this case
        long count = messageRepository.countByCaseIdAndReceiverIdAndReceiverTypeAndIsRead(
            caseId, currentUserId, currentUserType, false);

        logger.info("Unread count for case {} for user {}/{}: {}", caseId, currentUserType, currentUserId, count);
        return count;
    }

    /**
     * Get current authenticated user from SecurityContext
     */
    private UserPrincipal getCurrentAuthenticatedUser() {
        org.springframework.security.core.Authentication auth = 
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal)) {
            throw new UnauthorizedException("User not authenticated");
        }
        
        return (UserPrincipal) auth.getPrincipal();
    }

    /**
     * Get case and validate receiver belongs to the same case
     */
    private Case getCaseAndValidateReceiver(Long caseId, Long receiverId, String receiverType) {
        Case caseEntity = caseRepository.findById(caseId)
            .orElseThrow(() -> new BadRequestException("Case not found with id: " + caseId));

        boolean receiverValid = false;
        
        if ("user".equalsIgnoreCase(receiverType)) {
            receiverValid = caseEntity.getUserId().equals(receiverId);
        } else if ("lawyer".equalsIgnoreCase(receiverType)) {
            receiverValid = caseEntity.getLawyerId() != null && 
                           caseEntity.getLawyerId().equals(receiverId);
        }

        if (!receiverValid) {
            logger.warn("Invalid receiver {}/{} for case {}: Case has User={}, Lawyer={}", 
                       receiverType, receiverId, caseId, 
                       caseEntity.getUserId(), caseEntity.getLawyerId());
            throw new BadRequestException("Receiver does not belong to this case");
        }

        return caseEntity;
    }

    /**
     * Broadcast message to case-specific WebSocket topic
     */
    private void broadcastMessageToCase(MessageDTO messageDTO) {
        try {
            String topic = "/topic/case/" + messageDTO.getCaseId();
            messagingTemplate.convertAndSend(topic, messageDTO);
            
            logger.debug("Message {} broadcasted to topic: {}", messageDTO.getId(), topic);
        } catch (Exception e) {
            logger.error("Failed to broadcast message {} to WebSocket: {}", 
                        messageDTO.getId(), e.getMessage());
            // Don't throw - message was saved successfully
        }
    }

    /**
     * Convert Message entity to MessageDTO
     */
    private MessageDTO convertToDTO(Message message) {
        return new MessageDTO(
            message.getId(),
            message.getCaseId(),
            message.getSenderId(),
            message.getSenderType(),
            message.getReceiverId(),
            message.getReceiverType(),
            message.getMessageText(),
            message.getIsRead(),
            message.getCreatedAt()
        );
    }

    /**
     * DEPRECATED: Get messages by receiver (removed for security)
     * 
     * This method is removed because it bypasses case-based authorization.
     * Use getMessagesByCaseId instead.
     */
    @Deprecated
    public List<MessageDTO> getMessagesByReceiver(Long receiverId, String receiverType) {
        logger.warn("Deprecated method called: getMessagesByReceiver - use case-based methods instead");
        throw new UnsupportedOperationException("Use case-based message retrieval instead");
    }

    /**
     * DEPRECATED: Get unread count by receiver (removed for security)
     * 
     * This method is removed because it bypasses case-based authorization.
     * Use getUnreadMessageCountForCase instead.
     */
    @Deprecated
    public long getUnreadMessageCount(Long receiverId, String receiverType) {
        logger.warn("Deprecated method called: getUnreadMessageCount - use case-based methods instead");
        throw new UnsupportedOperationException("Use case-based unread count instead");
    }
}

