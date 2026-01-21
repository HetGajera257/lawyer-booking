package com.legalconnect.lawyerbooking.controller;

import com.legalconnect.lawyerbooking.dto.LawyerProfileDTO;
import com.legalconnect.lawyerbooking.entity.Lawyer;
import com.legalconnect.lawyerbooking.exception.ResourceNotFoundException;
import com.legalconnect.lawyerbooking.repository.LawyerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lawyers")
@CrossOrigin(origins = "*")
public class LawyerController {

    @Autowired
    private LawyerRepository lawyerRepository;

    @Autowired
    private com.legalconnect.lawyerbooking.service.AuthorizationService authorizationService;

    @GetMapping("/{lawyerId}/profile")
    public ResponseEntity<LawyerProfileDTO> getLawyerProfile(@PathVariable("lawyerId") Long lawyerId) {
        Lawyer lawyer = lawyerRepository.findById(lawyerId)
                .orElseThrow(() -> new ResourceNotFoundException("Lawyer not found with ID: " + lawyerId));

        LawyerProfileDTO dto = new LawyerProfileDTO(
                lawyer.getId(),
                lawyer.getFullName(),
                lawyer.getSpecialization(),
                lawyer.getSpecializations(),
                lawyer.getYearsOfExperience(),
                lawyer.getLanguagesKnown(),
                lawyer.getRating(),
                lawyer.getCompletedCasesCount(),
                lawyer.getProfilePhotoUrl(),
                lawyer.getAvailabilityInfo(),
                lawyer.getBarNumber(),
                lawyer.getEmail()
        );

        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{lawyerId}/profile")
    public ResponseEntity<LawyerProfileDTO> updateLawyerProfile(@PathVariable("lawyerId") Long lawyerId, @RequestBody LawyerProfileDTO profileDTO) {
        authorizationService.verifyLawyerAccess(lawyerId);

        Lawyer lawyer = lawyerRepository.findById(lawyerId)
                .orElseThrow(() -> new ResourceNotFoundException("Lawyer not found with ID: " + lawyerId));

        // Update fields
        lawyer.setFullName(profileDTO.getFullName());
        lawyer.setSpecialization(profileDTO.getSpecialization());
        lawyer.setSpecializations(profileDTO.getSpecializations());
        lawyer.setYearsOfExperience(profileDTO.getYearsOfExperience());
        lawyer.setLanguagesKnown(profileDTO.getLanguagesKnown());
        lawyer.setAvailabilityInfo(profileDTO.getAvailabilityInfo());
        lawyer.setEmail(profileDTO.getEmail());
        lawyer.setProfilePhotoUrl(profileDTO.getProfilePhotoUrl());
        lawyer.setBarNumber(profileDTO.getBarNumber());

        Lawyer savedLawyer = lawyerRepository.save(lawyer);

        LawyerProfileDTO responseDTO = new LawyerProfileDTO(
                savedLawyer.getId(),
                savedLawyer.getFullName(),
                savedLawyer.getSpecialization(),
                savedLawyer.getSpecializations(),
                savedLawyer.getYearsOfExperience(),
                savedLawyer.getLanguagesKnown(),
                savedLawyer.getRating(),
                savedLawyer.getCompletedCasesCount(),
                savedLawyer.getProfilePhotoUrl(),
                savedLawyer.getAvailabilityInfo(),
                savedLawyer.getBarNumber(),
                savedLawyer.getEmail()
        );

        return ResponseEntity.ok(responseDTO);
    }
}
