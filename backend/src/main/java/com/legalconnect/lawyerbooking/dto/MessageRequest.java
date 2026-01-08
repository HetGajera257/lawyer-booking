package com.legalconnect.lawyerbooking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * SECURE MESSAGE REQUEST DTO
 * 
 * PURPOSE: Request DTO for sending messages in case-scoped chat
 * 
 * SECURITY NOTE: 
 * - Sender information (senderId, senderType) is NEVER accepted from the client
 * - Sender identity is derived from the JWT token by the backend
 * - This prevents identity spoofing attacks where users could pretend to be someone else
 * 
 * VALIDATION:
 * - All fields are validated to prevent malformed requests
 * - Case ID must exist and be accessible to the sender
 * - Receiver must belong to the same case
 * - Message text cannot be empty
 */
public class MessageRequest {
    
    @NotNull(message = "Case ID is required")
    private Long caseId;
    
    @NotNull(message = "Receiver ID is required")
    private Long receiverId;
    
    @NotBlank(message = "Receiver type is required")
    private String receiverType; // "user" or "lawyer" - validated against case participants
    
    @NotBlank(message = "Message text cannot be empty")
    private String messageText;
    
    // SECURITY: NO senderId or senderType fields
    // These are extracted from JWT by the backend to prevent identity spoofing
    
    // Constructors
    public MessageRequest() {}
    
    public MessageRequest(Long caseId, Long receiverId, String receiverType, String messageText) {
        this.caseId = caseId;
        this.receiverId = receiverId;
        this.receiverType = receiverType;
        this.messageText = messageText;
    }
    
    // Getters and Setters
    public Long getCaseId() {
        return caseId;
    }
    
    public void setCaseId(Long caseId) {
        this.caseId = caseId;
    }
    
    public Long getReceiverId() {
        return receiverId;
    }
    
    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }
    
    public String getReceiverType() {
        return receiverType;
    }
    
    public void setReceiverType(String receiverType) {
        this.receiverType = receiverType;
    }
    
    public String getMessageText() {
        return messageText;
    }
    
    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }
    
    /**
     * Validate receiver type
     */
    public boolean isValidReceiverType() {
        return "user".equalsIgnoreCase(receiverType) || "lawyer".equalsIgnoreCase(receiverType);
    }
    
    /**
     * Get normalized receiver type
     */
    public String getNormalizedReceiverType() {
        return receiverType != null ? receiverType.toLowerCase().trim() : null;
    }
    
    @Override
    public String toString() {
        return "MessageRequest{" +
                "caseId=" + caseId +
                ", receiverId=" + receiverId +
                ", receiverType='" + receiverType + '\'' +
                ", messageText='" + messageText + '\'' +
                '}';
    }
}
