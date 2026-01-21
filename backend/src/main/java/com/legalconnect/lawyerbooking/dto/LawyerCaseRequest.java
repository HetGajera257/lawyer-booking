package com.legalconnect.lawyerbooking.dto;

import java.io.Serializable;

public class LawyerCaseRequest implements Serializable {
    private Long caseId;
    private String title;
    private String category;
    private String type;
    private String description;
    private Long userId;
    private String createdAt;

    public LawyerCaseRequest() {}

    public LawyerCaseRequest(Long caseId, String title, String category, String type, String description, Long userId) {
        this.caseId = caseId;
        this.title = title;
        this.category = category;
        this.type = type;
        this.description = description;
        this.userId = userId;
        this.createdAt = java.time.LocalDateTime.now().toString();
    }

    public Long getCaseId() { return caseId; }
    public void setCaseId(Long caseId) { this.caseId = caseId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
