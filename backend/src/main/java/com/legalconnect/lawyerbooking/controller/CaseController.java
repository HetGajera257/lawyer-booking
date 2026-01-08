package com.legalconnect.lawyerbooking.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import com.legalconnect.lawyerbooking.service.CaseService;
import com.legalconnect.lawyerbooking.service.AuthorizationService;
import com.legalconnect.lawyerbooking.util.JwtUtil;
import com.legalconnect.lawyerbooking.dto.CaseDTO;
import com.legalconnect.lawyerbooking.dto.CaseRequest;
import com.legalconnect.lawyerbooking.dto.LawyerSearchCriteria;
import com.legalconnect.lawyerbooking.dto.LawyerSearchResponse;
import com.legalconnect.lawyerbooking.entity.Lawyer;
import com.legalconnect.lawyerbooking.service.LawyerService;
import com.legalconnect.lawyerbooking.exception.UnauthorizedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cases")
@CrossOrigin(origins = "*")
public class CaseController {

    private static final Logger logger = LoggerFactory.getLogger(CaseController.class);

    @Autowired
    private CaseService caseService;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private LawyerService lawyerService;

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("CaseController is active and reachable");
    }

    @GetMapping("/lawyers/ping")
    public ResponseEntity<String> pingLawyers() {
        return ResponseEntity.ok("Lawyer search through CaseController is active");
    }

    @GetMapping("/lawyers/search")
    public ResponseEntity<LawyerSearchResponse> searchLawyers(
            LawyerSearchCriteria criteria,
            @PageableDefault(size = 10, sort = "rating", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        try {
            logger.info("Searching lawyers via CaseController: {}", criteria);
            LawyerSearchResponse response = lawyerService.searchLawyers(criteria, pageable);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error searching lawyers: ", e);
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/create")
    public ResponseEntity<CaseDTO> createCase(@RequestBody CaseRequest request) {
        try {
            CaseDTO caseDTO = caseService.createCase(request);
            return ResponseEntity.ok(caseDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<CaseDTO> getCaseById(@PathVariable Long id) {
        try {
            authorizationService.verifyCaseAccess(id);
            CaseDTO caseDTO = caseService.getCaseById(id);
            return ResponseEntity.ok(caseDTO);
        } catch (UnauthorizedException e) {
            logger.warn("Unauthorized access attempt to case {}", id);
            return ResponseEntity.status(401).build();
        } catch (Exception e) {
            logger.error("Error fetching case {}", id, e);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CaseDTO>> getCasesByUserId(@PathVariable Long userId) {
        List<CaseDTO> cases = caseService.getCasesByUserId(userId);
        return ResponseEntity.ok(cases);
    }

    @GetMapping("/lawyer/{lawyerId}")
    public ResponseEntity<List<CaseDTO>> getCasesByLawyerId(@PathVariable Long lawyerId) {
        List<CaseDTO> cases = caseService.getCasesByLawyerId(lawyerId);
        return ResponseEntity.ok(cases);
    }

    @GetMapping("/unassigned")
    public ResponseEntity<List<CaseDTO>> getUnassignedCases() {
        List<CaseDTO> cases = caseService.getUnassignedCases();
        return ResponseEntity.ok(cases);
    }

    @GetMapping("/recommended/{lawyerId}")
    public ResponseEntity<List<CaseDTO>> getRecommendedCases(@PathVariable Long lawyerId) {
        List<CaseDTO> cases = caseService.getRecommendedCases(lawyerId);
        return ResponseEntity.ok(cases);
    }

    @PostMapping("/{caseId}/assign")
    public ResponseEntity<CaseDTO> assignLawyerToCase(
            @PathVariable Long caseId,
            @RequestBody Map<String, Long> request) {
        try {
            Long lawyerId = request.get("lawyerId");
            CaseDTO caseDTO = caseService.assignLawyerToCase(caseId, lawyerId);
            return ResponseEntity.ok(caseDTO);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{caseId}/solution")
    public ResponseEntity<CaseDTO> updateCaseSolution(
            @PathVariable Long caseId,
            @RequestBody Map<String, String> request) {
        try {
            authorizationService.verifyCaseUpdateAccess(caseId);
            
            String solution = request.get("solution");
            CaseDTO caseDTO = caseService.updateCaseSolution(caseId, solution);
            return ResponseEntity.ok(caseDTO);
        } catch (UnauthorizedException e) {
            logger.warn("Unauthorized attempt to update case solution {}", caseId);
            return ResponseEntity.status(401).build();
        } catch (Exception e) {
            logger.error("Error updating case solution {}", caseId, e);
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{caseId}/status")
    public ResponseEntity<CaseDTO> updateCaseStatus(
            @PathVariable Long caseId,
            @RequestBody Map<String, String> request) {
        try {
            String status = request.get("status");
            CaseDTO caseDTO = caseService.updateCaseStatus(caseId, status);
            return ResponseEntity.ok(caseDTO);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

