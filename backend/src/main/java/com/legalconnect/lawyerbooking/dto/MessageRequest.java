package com.legalconnect.lawyerbooking.dto;

public class MessageRequest {
    private Long caseId;
    private Long senderId;
    private String senderType;
    private Long receiverId;
    private String receiverType;
    private String messageText;

    // Constructors
    public MessageRequest() {}

    public MessageRequest(Long caseId, Long senderId, String senderType,
                         Long receiverId, String receiverType, String messageText) {
        this.caseId = caseId;
        this.senderId = senderId;
        this.senderType = senderType;
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
}

