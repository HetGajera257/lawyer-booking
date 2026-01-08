package com.legalconnect.lawyerbooking.dto;

public class LawyerSearchCriteria {
    private String specialization;
    private Double minRating;
    private Integer minExperience;
    private Integer minCompletedCases;
    private String availability;
    private String name;

    // Getters and Setters
    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public Double getMinRating() { return minRating; }
    public void setMinRating(Double minRating) { this.minRating = minRating; }

    public Integer getMinExperience() { return minExperience; }
    public void setMinExperience(Integer minExperience) { this.minExperience = minExperience; }

    public Integer getMinCompletedCases() { return minCompletedCases; }
    public void setMinCompletedCases(Integer minCompletedCases) { this.minCompletedCases = minCompletedCases; }

    public String getAvailability() { return availability; }
    public void setAvailability(String availability) { this.availability = availability; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public String toString() {
        return "LawyerSearchCriteria{" +
                "specialization='" + specialization + '\'' +
                ", minRating=" + minRating +
                ", minExperience=" + minExperience +
                ", minCompletedCases=" + minCompletedCases +
                ", availability='" + availability + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
