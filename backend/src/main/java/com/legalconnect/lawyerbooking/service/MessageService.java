package com.legalconnect.lawyerbooking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.legalconnect.lawyerbooking.entity.Message;
import com.legalconnect.lawyerbooking.exception.BadRequestException;
import com.legalconnect.lawyerbooking.repository.MessageRepository;
import com.legalconnect.lawyerbooking.dto.MessageDTO;
import com.legalconnect.lawyerbooking.dto.MessageRequest;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageService {

    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private AuthorizationService authorizationService;

    public MessageDTO sendMessage(MessageRequest request) {
        // Validate message text
        if (request.getMessageText() == null || request.getMessageText().trim().isEmpty()) {
            throw new BadRequestException("Message text cannot be empty");
        }

        // Verify sender has access to the case
        authorizationService.verifyMessageAccess(
            request.getCaseId(), 
            request.getSenderId(), 
            request.getSenderType()
        );

        logger.info("Sending message from {} {} to {} {} for case {}", 
                   request.getSenderType(), request.getSenderId(),
                   request.getReceiverType(), request.getReceiverId(),
                   request.getCaseId());

        Message message = new Message();
        message.setCaseId(request.getCaseId());
        message.setSenderId(request.getSenderId());
        message.setSenderType(request.getSenderType());
        message.setReceiverId(request.getReceiverId());
        message.setReceiverType(request.getReceiverType());
        message.setMessageText(request.getMessageText().trim());
        message.setIsRead(false);
        
        Message saved = messageRepository.save(message);
        return convertToDTO(saved);
    }

    public List<MessageDTO> getMessagesByCaseId(Long caseId) {
        List<Message> messages = messageRepository.findByCaseIdOrderByCreatedAtAsc(caseId);
        return messages.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<MessageDTO> getMessagesByReceiver(Long receiverId, String receiverType) {
        List<Message> messages = messageRepository.findByReceiverIdAndReceiverType(receiverId, receiverType);
        return messages.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public void markMessageAsRead(Long messageId) {
        Message message = messageRepository.findById(messageId)
            .orElseThrow(() -> new RuntimeException("Message not found with id: " + messageId));
        message.setIsRead(true);
        messageRepository.save(message);
    }

    public long getUnreadMessageCount(Long receiverId, String receiverType) {
        return messageRepository.countByReceiverIdAndReceiverTypeAndIsRead(receiverId, receiverType, false);
    }

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
}

