package com.legalconnect.lawyerbooking.service;

import com.legalconnect.lawyerbooking.dto.LawyerSearchCriteria;
import com.legalconnect.lawyerbooking.entity.Lawyer;
import com.legalconnect.lawyerbooking.repository.LawyerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.legalconnect.lawyerbooking.dto.LawyerDTO;
import com.legalconnect.lawyerbooking.dto.LawyerSearchResponse;

/**
 * OPTIMIZED LAWYER SERVICE
 * 
 * PERFORMANCE IMPROVEMENTS:
 * - Cached lawyer search results
 * - Cached lawyer profiles
 * - Optimized JPA queries
 * - Pagination support
 * - Efficient DTO conversion
 */
@Service
public class LawyerService {

    @Autowired
    private LawyerRepository lawyerRepository;

    /**
     * Search lawyers with caching and pagination
     * 
     * PERFORMANCE: Cached for 5 minutes to reduce database load
     * SECURITY: Returns DTOs, never entities
     */
    @Cacheable(value = "lawyerSearch", key = "#criteria.toString() + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public LawyerSearchResponse searchLawyers(LawyerSearchCriteria criteria, Pageable pageable) {
        Page<Lawyer> page = lawyerRepository.findAll((Specification<Lawyer>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter by ACTIVE status or handle NULL for existing records
            predicates.add(cb.or(
                cb.equal(root.get("accountStatus"), "ACTIVE"),
                cb.isNull(root.get("accountStatus"))
            ));

            if (criteria.getName() != null && !criteria.getName().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("fullName")), "%" + criteria.getName().toLowerCase() + "%"));
            }

            if (criteria.getSpecialization() != null && !criteria.getSpecialization().isEmpty()) {
                String specPattern = "%" + criteria.getSpecialization().toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("specializations")), specPattern),
                    cb.like(cb.lower(root.get("specialization")), specPattern)
                ));
            }

            if (criteria.getMinRating() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("rating"), criteria.getMinRating()));
            }

            if (criteria.getMinExperience() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("yearsOfExperience"), criteria.getMinExperience()));
            }

            if (criteria.getMinCompletedCases() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("completedCasesCount"), criteria.getMinCompletedCases()));
            }

            if (criteria.getAvailability() != null && !criteria.getAvailability().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("availabilityInfo")), "%" + criteria.getAvailability().toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);
        
        // Convert entities to DTOs (SECURITY: prevents password exposure)
        List<LawyerDTO> dtos = page.getContent().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        
        return new LawyerSearchResponse(
            dtos,
            page.getNumber(),
            page.getTotalPages(),
            page.getTotalElements(),
            page.getSize()
        );
    }
    
    /**
     * Get lawyer profile by ID with caching
     * 
     * PERFORMANCE: Cached for 10 minutes
     * SECURITY: Returns DTO, never entity with password
     */
    @Cacheable(value = "lawyerProfiles", key = "#lawyerId")
    public LawyerDTO getLawyerProfile(Long lawyerId) {
        Lawyer lawyer = lawyerRepository.findById(lawyerId)
            .orElseThrow(() -> new RuntimeException("Lawyer not found with id: " + lawyerId));
        return convertToDTO(lawyer);
    }
    
    /**
     * Update lawyer profile with cache eviction
     * 
     * PERFORMANCE: Evicts cached profile and search results
     */
    @CacheEvict(value = {"lawyerProfiles", "lawyerSearch"}, key = "#lawyerId")
    public LawyerDTO updateLawyerProfile(Long lawyerId, LawyerDTO updateData) {
        Lawyer lawyer = lawyerRepository.findById(lawyerId)
            .orElseThrow(() -> new RuntimeException("Lawyer not found with id: " + lawyerId));
        
        // Update fields (only allowed fields)
        if (updateData.getFullName() != null) {
            lawyer.setFullName(updateData.getFullName());
        }
        if (updateData.getSpecialization() != null) {
            lawyer.setSpecialization(updateData.getSpecialization());
        }
        if (updateData.getSpecializations() != null) {
            lawyer.setSpecializations(updateData.getSpecializations());
        }
        if (updateData.getYearsOfExperience() != null) {
            lawyer.setYearsOfExperience(updateData.getYearsOfExperience());
        }
        if (updateData.getAvailabilityInfo() != null) {
            lawyer.setAvailabilityInfo(updateData.getAvailabilityInfo());
        }
        if (updateData.getLanguagesKnown() != null) {
            lawyer.setLanguagesKnown(updateData.getLanguagesKnown());
        }
        if (updateData.getProfilePhotoUrl() != null) {
            lawyer.setProfilePhotoUrl(updateData.getProfilePhotoUrl());
        }
        
        Lawyer savedLawyer = lawyerRepository.save(lawyer);
        return convertToDTO(savedLawyer);
    }
    
    /**
     * Evict lawyer cache when rating changes
     * 
     * PERFORMANCE: Updates cached search results
     */
    @CacheEvict(value = {"lawyerProfiles", "lawyerSearch"}, key = "#lawyerId")
    public void updateLawyerRating(Long lawyerId, Double newRating) {
        Lawyer lawyer = lawyerRepository.findById(lawyerId)
            .orElseThrow(() -> new RuntimeException("Lawyer not found with id: " + lawyerId));
        
        lawyer.setRating(newRating);
        lawyerRepository.save(lawyer);
    }
    
    /**
     * Converts Lawyer entity to LawyerDTO.
     * 
     * SECURITY: Excludes sensitive fields like password.
     * PERFORMANCE: Efficient field mapping
     */
    private LawyerDTO convertToDTO(Lawyer lawyer) {
        LawyerDTO dto = new LawyerDTO();
        dto.setId(lawyer.getId());
        dto.setFullName(lawyer.getFullName());
        dto.setSpecialization(lawyer.getSpecialization());
        dto.setSpecializations(lawyer.getSpecializations());
        dto.setYearsOfExperience(lawyer.getYearsOfExperience());
        dto.setRating(lawyer.getRating());
        dto.setCompletedCasesCount(lawyer.getCompletedCasesCount());
        dto.setTotalCasesCount(lawyer.getTotalCasesCount());
        dto.setAvailabilityInfo(lawyer.getAvailabilityInfo());
        dto.setAccountStatus(lawyer.getAccountStatus());
        dto.setEmail(lawyer.getEmail());
        dto.setBarNumber(lawyer.getBarNumber());
        dto.setLanguagesKnown(lawyer.getLanguagesKnown());
        dto.setProfilePhotoUrl(lawyer.getProfilePhotoUrl());
        // NO password field - security critical
        return dto;
    }
}
