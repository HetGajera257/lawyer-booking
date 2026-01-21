package com.legalconnect.lawyerbooking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.legalconnect.lawyerbooking.entity.Case;
import com.legalconnect.lawyerbooking.exception.BadRequestException;
import com.legalconnect.lawyerbooking.exception.ResourceNotFoundException;
import com.legalconnect.lawyerbooking.repository.CaseRepository;
import com.legalconnect.lawyerbooking.repository.LawyerRepository;
import com.legalconnect.lawyerbooking.repository.ClientAudioRepository;
import com.legalconnect.lawyerbooking.dto.CaseDTO;
import com.legalconnect.lawyerbooking.dto.CaseRequest;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CaseService {

    private static final Logger logger = LoggerFactory.getLogger(CaseService.class);

    private static final Set<String> VALID_STATUSES = Set.of(
        "open", "in-progress", "closed", "on-hold"
    );

    @Autowired
    private CaseRepository caseRepository;

    @Autowired
    private LawyerRepository lawyerRepository;

    @Autowired
    private CaseClassificationService classificationService;

    @Autowired
    private ClientAudioRepository clientAudioRepository;

    @Autowired
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    public CaseDTO createCase(CaseRequest request) {
        Case caseEntity = new Case();
        caseEntity.setUserId(request.getUserId());
        caseEntity.setCaseTitle(request.getCaseTitle());
        caseEntity.setCaseType(request.getCaseType());
        
        // Automatic Classification if not provided
        String category = request.getCaseCategory();
        if (category == null || category.trim().isEmpty()) {
            category = classificationService.classifyCase(request.getDescription());
            logger.info("Automatically classified case as: {}", category);
        }
        caseEntity.setCaseCategory(category);
        
        caseEntity.setDescription(request.getDescription());
        caseEntity.setCaseStatus("open");
        
        Case saved = caseRepository.save(caseEntity);
        CaseDTO dto = convertToDTO(saved);
        
        // 7. Automatic Lawyer Request (Requirement: "send request not notification")
        try {
            com.legalconnect.lawyerbooking.dto.LawyerCaseRequest requestPayload = new com.legalconnect.lawyerbooking.dto.LawyerCaseRequest(
                dto.getId(),
                dto.getCaseTitle(),
                dto.getCaseCategory(),
                dto.getCaseType(),
                dto.getDescription(),
                dto.getUserId()
            );
            
            System.out.println(">>> [WS BROADCAST START] Case ID: " + dto.getId());
            System.out.println(">>> [WS BROADCAST CATEGORY]: " + (requestPayload.getCategory() != null ? requestPayload.getCategory() : "NULL"));
            
            if (messagingTemplate == null) {
                System.err.println(">>> [WS BROADCAST ERROR] messagingTemplate is NULL!");
                logger.error("CRITICAL: messagingTemplate is NULL in CaseService!");
            } else {
                logger.info("DEBUG: Attempting to send LawyerCaseRequest to /topic/lawyer/requests for Case ID: {}. Category: {}", requestPayload.getCaseId(), requestPayload.getCategory());
                messagingTemplate.convertAndSend("/topic/lawyer/requests", requestPayload);
                System.out.println(">>> [WS BROADCAST SUCCESS] Sent to /topic/lawyer/requests for Case: " + dto.getId());
                logger.info("SUCCESS: Sent new case request for case ID: {}", dto.getId());
            }
        } catch (Exception e) {
            System.err.println(">>> [WS BROADCAST FAILED] Error: " + e.getMessage());
            e.printStackTrace();
            logger.error("Failed to send lawyer request: {}", e.getMessage());
        }
        
        return dto;
    }

    public CaseDTO getCaseById(Long id) {
        Case caseEntity = caseRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Case not found with id: " + id));
        return convertToDTO(caseEntity);
    }

    public List<CaseDTO> getCasesByUserId(Long userId) {
        List<Case> cases = caseRepository.findByUserId(userId);
        return cases.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<CaseDTO> getCasesByLawyerId(Long lawyerId) {
        List<Case> cases = caseRepository.findByLawyerId(lawyerId);
        return cases.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<CaseDTO> getUnassignedCases() {
        List<Case> cases = caseRepository.findByLawyerIdIsNull();
        return cases.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<CaseDTO> getRecommendedCases(Long lawyerId) {
        var lawyer = lawyerRepository.findById(lawyerId)
            .orElseThrow(() -> new ResourceNotFoundException("Lawyer not found"));
            
        String specs = lawyer.getSpecializations();
        if (specs == null || specs.trim().isEmpty()) {
            return getUnassignedCases();
        }
        
        java.util.List<String> categories = java.util.Arrays.asList(specs.split("\\s*,\\s*"));
        List<Case> cases = caseRepository.findByLawyerIdIsNullAndCaseCategoryIn(categories);
        return cases.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @org.springframework.transaction.annotation.Transactional
    public CaseDTO assignLawyerToCase(Long caseId, Long lawyerId) {
        Case caseEntity = caseRepository.findById(caseId)
            .orElseThrow(() -> new ResourceNotFoundException("Case not found with id: " + caseId));
            
        var lawyer = lawyerRepository.findById(lawyerId)
            .orElseThrow(() -> new ResourceNotFoundException("Lawyer not found"));

        // Validation: Prevent re-assignment if already assigned
        if (caseEntity.getLawyerId() != null) {
            if (caseEntity.getLawyerId().equals(lawyerId)) {
                return convertToDTO(caseEntity); // Already assigned to this lawyer
            }
            logger.warn("Lawyer {} attempted to claim case {} which is already assigned to lawyer {}", 
                        lawyerId, caseId, caseEntity.getLawyerId());
            throw new BadRequestException("This case has already been accepted by another lawyer");
        }

        // Validation: Ensure lawyer specialization matches case category (if both exist)
        String category = caseEntity.getCaseCategory();
        String specs = lawyer.getSpecializations();
        
        if (category != null && specs != null && !specs.toLowerCase().contains(category.toLowerCase())) {
            logger.warn("Lawyer {} (specs: {}) specialized areas do not match case category: {}", 
                        lawyerId, specs, category);
        }

        caseEntity.setLawyerId(lawyerId);
        caseEntity.setCaseStatus("in-progress");
        Case updated = caseRepository.save(caseEntity);
        CaseDTO dto = convertToDTO(updated);

        // SYNC: Update any linked ClientAudio record so it reflects the assigned lawyer
        try {
            java.util.List<com.legalconnect.lawyerbooking.entity.ClientAudio> audios = 
                clientAudioRepository.findByCaseId(caseId);
            for (com.legalconnect.lawyerbooking.entity.ClientAudio audio : audios) {
                audio.setLawyerId(lawyerId);
                clientAudioRepository.save(audio);
                logger.info("Updated ClientAudio {} with lawyerId {}", audio.getId(), lawyerId);
            }
        } catch (Exception e) {
            logger.error("Failed to sync ClientAudio with case assignment: {}", e.getMessage());
        }

        // Broadcast that a case has been taken (to refresh other lawyers' lists)
        try {
            messagingTemplate.convertAndSend("/topic/lawyer/updates", Map.of(
                "type", "CASE_ASSIGNED",
                "caseId", caseId,
                "lawyerId", lawyerId
            ));
        } catch (Exception e) {
            logger.error("Failed to broadcast case assignment: {}", e.getMessage());
        }

        return dto;
    }

    public CaseDTO updateCaseSolution(Long caseId, String solution) {
        Case caseEntity = caseRepository.findById(caseId)
            .orElseThrow(() -> new RuntimeException("Case not found with id: " + caseId));
        caseEntity.setSolution(solution);
        Case updated = caseRepository.save(caseEntity);
        CaseDTO dto = convertToDTO(updated);
        
        // Broadcast update to real-time subscribers
        messagingTemplate.convertAndSend("/topic/case/" + caseId, dto);
        
        return dto;
    }

    public CaseDTO updateCaseStatus(Long caseId, String status) {
        if (status == null || !VALID_STATUSES.contains(status.toLowerCase())) {
            throw new BadRequestException("Invalid case status: " + status + 
                ". Valid statuses are: " + VALID_STATUSES);
        }

        Case caseEntity = caseRepository.findById(caseId)
            .orElseThrow(() -> new ResourceNotFoundException("Case not found with id: " + caseId));
        
        logger.info("Updating case {} status from {} to {}", caseId, caseEntity.getCaseStatus(), status);
        caseEntity.setCaseStatus(status.toLowerCase());
        Case updated = caseRepository.save(caseEntity);
        CaseDTO dto = convertToDTO(updated);
        
        // Broadcast update
        messagingTemplate.convertAndSend("/topic/case/" + caseId, dto);
        
        return dto;
    }

    private CaseDTO convertToDTO(Case caseEntity) {
        return new CaseDTO(
            caseEntity.getId(),
            caseEntity.getUserId(),
            caseEntity.getLawyerId(),
            caseEntity.getCaseTitle(),
            caseEntity.getCaseType(),
            caseEntity.getCaseStatus(),
            caseEntity.getDescription(),
            caseEntity.getCaseCategory(),
            caseEntity.getSolution(),
            caseEntity.getCreatedAt(),
            caseEntity.getUpdatedAt()
        );
    }
}

