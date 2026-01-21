package com.legalconnect.lawyerbooking.dto;

import java.util.List;

/**
 * Response wrapper for lawyer search results.
 * 
 * Provides pagination metadata and prevents direct exposure of Spring Data Page objects.
 * This follows the DTO pattern and gives us control over the API contract.
 */
public class LawyerSearchResponse {
    private List<LawyerDTO> lawyers;
    private int currentPage;
    private int totalPages;
    private long totalElements;
    private int pageSize;
    
    // Constructors
    public LawyerSearchResponse() {}
    
    public LawyerSearchResponse(List<LawyerDTO> lawyers, int currentPage, int totalPages, 
                                long totalElements, int pageSize) {
        this.lawyers = lawyers;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
        this.pageSize = pageSize;
    }
    
    // Getters and Setters
    public List<LawyerDTO> getLawyers() {
        return lawyers;
    }
    
    public void setLawyers(List<LawyerDTO> lawyers) {
        this.lawyers = lawyers;
    }
    
    public int getCurrentPage() {
        return currentPage;
    }
    
    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }
    
    public int getTotalPages() {
        return totalPages;
    }
    
    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
    
    public long getTotalElements() {
        return totalElements;
    }
    
    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }
    
    public int getPageSize() {
        return pageSize;
    }
    
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
