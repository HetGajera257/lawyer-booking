package com.legalconnect.lawyerbooking.dto;

public class LawyerProfileDTO {
    private Long id;
    private String fullName;
    private String specialization;
    private String specializations;
    private Integer yearsOfExperience;
    private String languagesKnown;
    private Double rating;
    private Integer completedCasesCount;
    private String profilePhotoUrl;
    private String availabilityInfo;
    private String barNumber;
    private String email;

    public LawyerProfileDTO() {}

    public LawyerProfileDTO(Long id, String fullName, String specialization, String specializations,
                            Integer yearsOfExperience, String languagesKnown, Double rating,
                            Integer completedCasesCount, String profilePhotoUrl, String availabilityInfo,
                            String barNumber, String email) {
        this.id = id;
        this.fullName = fullName;
        this.specialization = specialization;
        this.specializations = specializations;
        this.yearsOfExperience = yearsOfExperience;
        this.languagesKnown = languagesKnown;
        this.rating = rating;
        this.completedCasesCount = completedCasesCount;
        this.profilePhotoUrl = profilePhotoUrl;
        this.availabilityInfo = availabilityInfo;
        this.barNumber = barNumber;
        this.email = email;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

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

    public String getBarNumber() { return barNumber; }
    public void setBarNumber(String barNumber) { this.barNumber = barNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
