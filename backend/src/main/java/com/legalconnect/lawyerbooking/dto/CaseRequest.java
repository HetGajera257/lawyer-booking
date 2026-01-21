package com.legalconnect.lawyerbooking.dto;

public class CaseRequest {
    private Long userId;
    private String caseTitle;
    private String caseType;
    private String description;
    private String caseCategory;

    // Constructors
    public CaseRequest() {}

    public CaseRequest(Long userId, String caseTitle, String caseType, String description, String caseCategory) {
        this.userId = userId;
        this.caseTitle = caseTitle;
        this.caseType = caseType;
        this.description = description;
        this.caseCategory = caseCategory;
    }

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCaseCategory() {
        return caseCategory;
    }

    public void setCaseCategory(String caseCategory) {
        this.caseCategory = caseCategory;
    }
}

