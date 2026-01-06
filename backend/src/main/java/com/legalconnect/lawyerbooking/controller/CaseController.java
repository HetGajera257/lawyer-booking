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
import com.legalconnect.lawyerbooking.exception.UnauthorizedException;

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
    public ResponseEntity<CaseDTO> getCaseById(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new UnauthorizedException("Authorization token required");
            }
            
            String token = authHeader.substring(7);
            authorizationService.verifyCaseAccess(id, token);
            
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
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, String> request) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new UnauthorizedException("Authorization token required");
            }
            
            String token = authHeader.substring(7);
            Long userId = jwtUtil.extractUserId(token);
            String userType = jwtUtil.extractUserType(token);
            
            authorizationService.verifyCaseUpdateAccess(caseId, userId, userType);
            
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

