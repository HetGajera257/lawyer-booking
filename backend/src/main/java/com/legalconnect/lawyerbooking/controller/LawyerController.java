package com.legalconnect.lawyerbooking.controller;

import com.legalconnect.lawyerbooking.dto.LawyerSearchCriteria;
import com.legalconnect.lawyerbooking.dto.LawyerDTO;
import com.legalconnect.lawyerbooking.dto.LawyerSearchResponse;
import com.legalconnect.lawyerbooking.service.LawyerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/lawyers")
@CrossOrigin(origins = "*")
public class LawyerController {

    @Autowired
    private LawyerService lawyerService;

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(LawyerController.class);

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("LawyerController is active");
    }

    /**
     * Get lawyer profile by ID.
     * Returns LawyerDTO (excludes password for security).
     */
    @GetMapping("/{id}/profile")
    public ResponseEntity<?> getLawyerProfile(@PathVariable Long id) {
        try {
            logger.info("Fetching lawyer profile for ID: {}", id);
            LawyerDTO profile = lawyerService.getLawyerProfile(id);
            return ResponseEntity.ok(profile);
        } catch (RuntimeException e) {
            logger.error("Error fetching lawyer profile for ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(404).body(Map.of("error", "Lawyer not found"));
        } catch (Exception e) {
            logger.error("Unexpected error fetching lawyer profile: ", e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Search lawyers with filters and pagination.
     * Returns LawyerSearchResponse with DTOs (excludes passwords).
     */
    @GetMapping("/search")
    public ResponseEntity<LawyerSearchResponse> searchLawyers(
            LawyerSearchCriteria criteria,
            @PageableDefault(size = 10, sort = "rating", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        try {
            logger.info("Searching lawyers with criteria: {}", criteria);
            LawyerSearchResponse response = lawyerService.searchLawyers(criteria, pageable);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error searching lawyers: ", e);
            return ResponseEntity.status(500).build();
        }
    }
}
