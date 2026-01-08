package com.legalconnect.lawyerbooking.service;

import com.legalconnect.lawyerbooking.entity.Case;
import com.legalconnect.lawyerbooking.entity.Appointment;
import com.legalconnect.lawyerbooking.entity.Message;
import com.legalconnect.lawyerbooking.exception.UnauthorizedException;
import com.legalconnect.lawyerbooking.exception.ResourceNotFoundException;
import com.legalconnect.lawyerbooking.repository.CaseRepository;
import com.legalconnect.lawyerbooking.repository.AppointmentRepository;
import com.legalconnect.lawyerbooking.repository.MessageRepository;
import com.legalconnect.lawyerbooking.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for handling authorization checks
 */
@Service
public class AuthorizationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationService.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CaseRepository caseRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private MessageRepository messageRepository;

    private com.legalconnect.lawyerbooking.security.UserPrincipal getCurrentUser() {
        org.springframework.security.core.Authentication auth = 
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.legalconnect.lawyerbooking.security.UserPrincipal) {
            return (com.legalconnect.lawyerbooking.security.UserPrincipal) auth.getPrincipal();
        }
        throw new com.legalconnect.lawyerbooking.exception.UnauthorizedException("User not authenticated");
    }

    /**
     * Verifies that the current user/lawyer has access to a specific case
     */
    public void verifyCaseAccess(Long caseId) {
        com.legalconnect.lawyerbooking.security.UserPrincipal currentUser = getCurrentUser();
        Long userId = currentUser.getUserId();
        String userType = currentUser.getUserType();

        Case caseEntity = caseRepository.findById(caseId)
            .orElseThrow(() -> new ResourceNotFoundException("Case not found with id: " + caseId));

        if (userType.equals("user")) {
            if (!caseEntity.getUserId().equals(userId)) {
                logger.warn("User {} attempted to access case {} owned by user {}", 
                           userId, caseId, caseEntity.getUserId());
                throw new UnauthorizedException("You can only access your own cases");
            }
        } else if (userType.equals("lawyer")) {
            // Lawyers can view unassigned cases or cases assigned to them
            if (caseEntity.getLawyerId() != null && !caseEntity.getLawyerId().equals(userId)) {
                logger.warn("Lawyer {} attempted to access case {} assigned to lawyer {}", 
                           userId, caseId, caseEntity.getLawyerId());
                throw new UnauthorizedException("You can only access cases assigned to you");
            }
        } else {
            throw new UnauthorizedException("Invalid user type");
        }
    }

    /**
     * Verifies that the current user/lawyer can send a message for a specific case
     */
    public void verifyMessageAccess(Long caseId) {
        com.legalconnect.lawyerbooking.security.UserPrincipal currentUser = getCurrentUser();
        Long senderId = currentUser.getUserId();
        String senderType = currentUser.getUserType();

        Case caseEntity = caseRepository.findById(caseId)
            .orElseThrow(() -> new ResourceNotFoundException("Case not found with id: " + caseId));

        if (senderType.equals("user")) {
            if (!caseEntity.getUserId().equals(senderId)) {
                throw new UnauthorizedException("User does not have access to this case");
            }
        } else if (senderType.equals("lawyer")) {
            if (caseEntity.getLawyerId() == null || !caseEntity.getLawyerId().equals(senderId)) {
                throw new UnauthorizedException("Lawyer does not have access to this case");
            }
        } else {
            throw new UnauthorizedException("Invalid sender type");
        }
    }

    /**
     * Verifies that a user can access an appointment
     */
    public void verifyAppointmentAccess(Long appointmentId) {
        com.legalconnect.lawyerbooking.security.UserPrincipal currentUser = getCurrentUser();
        Long userId = currentUser.getUserId();
        String userType = currentUser.getUserType();

        Appointment appointment = appointmentRepository.findById(appointmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        if (userType.equals("user")) {
            if (!appointment.getUserId().equals(userId)) {
                throw new UnauthorizedException("You can only access your own appointments");
            }
        } else if (userType.equals("lawyer")) {
            if (!appointment.getLawyerId().equals(userId)) {
                throw new UnauthorizedException("You can only access your own appointments");
            }
        }
    }

    /**
     * Verifies that a user can update a case (only assigned lawyer can update)
     */
    public void verifyCaseUpdateAccess(Long caseId) {
        com.legalconnect.lawyerbooking.security.UserPrincipal currentUser = getCurrentUser();
        Long userId = currentUser.getUserId();
        String userType = currentUser.getUserType();

        Case caseEntity = caseRepository.findById(caseId)
            .orElseThrow(() -> new ResourceNotFoundException("Case not found"));

        if (userType.equals("lawyer")) {
            if (caseEntity.getLawyerId() == null || !caseEntity.getLawyerId().equals(userId)) {
                throw new UnauthorizedException("You can only update cases assigned to you");
            }
        } else {
            throw new UnauthorizedException("Only assigned lawyers can update cases");
        }
    }
}
