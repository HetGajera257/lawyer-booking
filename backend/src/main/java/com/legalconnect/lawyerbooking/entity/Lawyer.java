package com.legalconnect.lawyerbooking.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lawyers")
public class Lawyer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(length = 255)
    private String email;

    @Column(name = "full_name", length = 255)
    private String fullName;

    @Column(name = "bar_number", length = 100)
    private String barNumber;

    @Column(length = 255)
    private String specialization;

    @Column(name = "specializations", length = 500)
    private String specializations; 

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    @Column(name = "languages_known", length = 255)
    private String languagesKnown;

    @Column(name = "rating")
    private Double rating;

    @Column(name = "completed_cases_count")
    private Integer completedCasesCount;

    @Column(name = "profile_photo_url", length = 500)
    private String profilePhotoUrl;

    @Column(name = "account_status", length = 20)
    private String accountStatus = "ACTIVE"; // ACTIVE, INACTIVE

    @Column(name = "total_cases_count")
    private Integer totalCasesCount = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "availability_info", length = 500)
    private String availabilityInfo;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getBarNumber() { return barNumber; }
    public void setBarNumber(String barNumber) { this.barNumber = barNumber; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getSpecializations() { return specializations; }
    public void setSpecializations(String specializations) { this.specializations = specializations; }

    public Integer getYearsOfExperience() { return yearsOfExperience; }
    public void setYearsOfExperience(Integer yearsOfExperience) { this.yearsOfExperience = yearsOfExperience; }

    public String getLanguagesKnown() { return languagesKnown; }
    public void setLanguagesKnown(String languagesKnown) { this.languagesKnown = languagesKnown; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public Integer getCompletedCasesCount() { return completedCasesCount; }
    public void setCompletedCasesCount(Integer completedCasesCount) { this.completedCasesCount = completedCasesCount; }

    public String getProfilePhotoUrl() { return profilePhotoUrl; }
    public void setProfilePhotoUrl(String profilePhotoUrl) { this.profilePhotoUrl = profilePhotoUrl; }

    public String getAvailabilityInfo() { return availabilityInfo; }
    public void setAvailabilityInfo(String availabilityInfo) { this.availabilityInfo = availabilityInfo; }

    public String getAccountStatus() { return accountStatus; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }

    public Integer getTotalCasesCount() { return totalCasesCount; }
    public void setTotalCasesCount(Integer totalCasesCount) { this.totalCasesCount = totalCasesCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

