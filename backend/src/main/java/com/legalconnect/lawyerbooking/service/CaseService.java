package com.legalconnect.lawyerbooking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.legalconnect.lawyerbooking.entity.Case;
import com.legalconnect.lawyerbooking.exception.BadRequestException;
import com.legalconnect.lawyerbooking.exception.ResourceNotFoundException;
import com.legalconnect.lawyerbooking.repository.CaseRepository;
import com.legalconnect.lawyerbooking.dto.CaseDTO;
import com.legalconnect.lawyerbooking.dto.CaseRequest;

import java.util.List;
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

    public CaseDTO createCase(CaseRequest request) {
        Case caseEntity = new Case();
        caseEntity.setUserId(request.getUserId());
        caseEntity.setCaseTitle(request.getCaseTitle());
        caseEntity.setCaseType(request.getCaseType());
        caseEntity.setDescription(request.getDescription());
        caseEntity.setCaseStatus("open");
        
        Case saved = caseRepository.save(caseEntity);
        return convertToDTO(saved);
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

    public CaseDTO assignLawyerToCase(Long caseId, Long lawyerId) {
        Case caseEntity = caseRepository.findById(caseId)
            .orElseThrow(() -> new RuntimeException("Case not found with id: " + caseId));
        caseEntity.setLawyerId(lawyerId);
        caseEntity.setCaseStatus("in-progress");
        Case updated = caseRepository.save(caseEntity);
        return convertToDTO(updated);
    }

    public CaseDTO updateCaseSolution(Long caseId, String solution) {
        Case caseEntity = caseRepository.findById(caseId)
            .orElseThrow(() -> new RuntimeException("Case not found with id: " + caseId));
        caseEntity.setSolution(solution);
        Case updated = caseRepository.save(caseEntity);
        return convertToDTO(updated);
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
        return convertToDTO(updated);
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
            caseEntity.getSolution(),
            caseEntity.getCreatedAt(),
            caseEntity.getUpdatedAt()
        );
    }
}

