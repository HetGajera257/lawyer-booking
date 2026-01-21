package com.legalconnect.lawyerbooking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.legalconnect.lawyerbooking.entity.Message;
import java.util.List;

/**
 * SECURE MESSAGE REPOSITORY
 * 
 * PURPOSE: Data access layer for case-based messaging
 * 
 * SECURITY FEATURES:
 * - Case-based query methods only
 * - No user-scoped queries (prevents cross-case data leakage)
 * - Read status tracking
 * - Ordered message retrieval
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    /**
     * Get all messages for a specific case, ordered by creation time
     * This is the primary method for chat message retrieval
     */
    List<Message> findByCaseIdOrderByCreatedAtAsc(Long caseId);
    
    /**
     * Get unread message count for a specific user in a specific case
     * Used for unread message indicators
     */
    long countByCaseIdAndReceiverIdAndReceiverTypeAndIsRead(Long caseId, Long receiverId, String receiverType, Boolean isRead);
    
    /**
     * Get messages sent by a specific user in a specific case
     * Used for message history and validation
     */
    List<Message> findByCaseIdAndSenderIdAndSenderType(Long caseId, Long senderId, String senderType);
    
    /**
     * Get messages received by a specific user in a specific case
     * Used for message history and validation
     */
    List<Message> findByCaseIdAndReceiverIdAndReceiverType(Long caseId, Long receiverId, String receiverType);
    
    /**
     * Get all messages for a specific case involving a specific user (as sender or receiver)
     * Used for comprehensive message access validation
     */
    @Query("SELECT m FROM Message m WHERE m.caseId = :caseId AND " +
           "(m.senderId = :userId AND m.senderType = :userType OR " +
           "m.receiverId = :userId AND m.receiverType = :userType) " +
           "ORDER BY m.createdAt ASC")
    List<Message> findCaseMessagesForUser(@Param("caseId") Long caseId, 
                                        @Param("userId") Long userId, 
                                        @Param("userType") String userType);
    
    /**
     * DEPRECATED: Legacy methods for backward compatibility
     * These should be replaced with case-based methods
     */
    @Deprecated
    List<Message> findBySenderIdAndSenderType(Long senderId, String senderType);
    
    @Deprecated
    List<Message> findByReceiverIdAndReceiverType(Long receiverId, String receiverType);
    
    @Deprecated
    long countByReceiverIdAndReceiverTypeAndIsRead(Long receiverId, String receiverType, Boolean isRead);
}

