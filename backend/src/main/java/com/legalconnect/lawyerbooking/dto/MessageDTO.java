package com.legalconnect.lawyerbooking.dto;

import java.time.LocalDateTime;

/**
 * SECURE MESSAGE DTO
 * 
 * PURPOSE: Data Transfer Object for messages with complete sender/receiver information
 * 
 * SECURITY FEATURES:
 * - Contains sender information for UI differentiation
 * - Includes case ID for scoping
 * - Read status for message tracking
 * - Timestamps for ordering
 * 
 * UI ENHANCEMENTS:
 * - Sender type for role-based styling
 * - Read status for message indicators
 * - Creation time for display
 * - Complete participant information
 */
public class MessageDTO {
    private Long id;
    private Long caseId;
    private Long senderId;
    private String senderType;
    private Long receiverId;
    private String receiverType;
    private String messageText;
    private Boolean isRead;
    private LocalDateTime createdAt;

    // Constructors
    public MessageDTO() {}

    public MessageDTO(Long id, Long caseId, Long senderId, String senderType,
                     Long receiverId, String receiverType, String messageText,
                     Boolean isRead, LocalDateTime createdAt) {
        this.id = id;
        this.caseId = caseId;
        this.senderId = senderId;
        this.senderType = senderType;
        this.receiverId = receiverId;
        this.receiverType = receiverType;
        this.messageText = messageText;
        this.isRead = isRead;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCaseId() {
        return caseId;
    }

    public void setCaseId(Long caseId) {
        this.caseId = caseId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getSenderType() {
        return senderType;
    }

    public void setSenderType(String senderType) {
        this.senderType = senderType;
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

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Check if sender is a user
     */
    public boolean isFromUser() {
        return "user".equalsIgnoreCase(senderType);
    }

    /**
     * Check if sender is a lawyer
     */
    public boolean isFromLawyer() {
        return "lawyer".equalsIgnoreCase(senderType);
    }

    /**
     * Get normalized sender type
     */
    public String getNormalizedSenderType() {
        return senderType != null ? senderType.toLowerCase() : null;
    }

    /**
     * Get normalized receiver type
     */
    public String getNormalizedReceiverType() {
        return receiverType != null ? receiverType.toLowerCase() : null;
    }

    /**
     * Get formatted creation time for display
     */
    public String getFormattedTime() {
        if (createdAt == null) return "";
        return createdAt.toString(); // Frontend will format this
    }

    @Override
    public String toString() {
        return "MessageDTO{" +
                "id=" + id +
                ", caseId=" + caseId +
                ", senderId=" + senderId +
                ", senderType='" + senderType + '\'' +
                ", receiverId=" + receiverId +
                ", receiverType='" + receiverType + '\'' +
                ", messageText='" + messageText + '\'' +
                ", isRead=" + isRead +
                ", createdAt=" + createdAt +
                '}';
    }
}

