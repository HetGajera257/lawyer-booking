package com.legalconnect.lawyerbooking.dto;

import java.time.LocalDateTime;

public class CaseDTO {
    private Long id;
    private Long userId;
    private Long lawyerId;
    private String caseTitle;
    private String caseType;
    private String caseStatus;
    private String description;
    private String caseCategory;
    private String solution;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public CaseDTO() {}

    public CaseDTO(Long id, Long userId, Long lawyerId, String caseTitle,
                   String caseType, String caseStatus, String description, 
                   String caseCategory, String solution, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.lawyerId = lawyerId;
        this.caseTitle = caseTitle;
        this.caseType = caseType;
        this.caseStatus = caseStatus;
        this.description = description;
        this.caseCategory = caseCategory;
        this.solution = solution;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getLawyerId() {
        return lawyerId;
    }

    public void setLawyerId(Long lawyerId) {
        this.lawyerId = lawyerId;
    }

    public String getCaseTitle() {
        return caseTitle;
    }

    public void setCaseTitle(String caseTitle) {
        this.caseTitle = caseTitle;
    }

    public String getCaseType() {
        return caseType;
    }

    public void setCaseType(String caseType) {
        this.caseType = caseType;
    }

    public String getCaseStatus() {
        return caseStatus;
    }

    public void setCaseStatus(String caseStatus) {
        this.caseStatus = caseStatus;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSolution() {
        return solution;
    }

    public void setSolution(String solution) {
        this.solution = solution;
    }

    public String getCaseCategory() {
        return caseCategory;
    }

    public void setCaseCategory(String caseCategory) {
        this.caseCategory = caseCategory;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

