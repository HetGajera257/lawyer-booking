package com.legalconnect.lawyerbooking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.legalconnect.lawyerbooking.entity.Message;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByCaseIdOrderByCreatedAtAsc(Long caseId);
    List<Message> findBySenderIdAndSenderType(Long senderId, String senderType);
    List<Message> findByReceiverIdAndReceiverType(Long receiverId, String receiverType);
    List<Message> findByCaseIdAndSenderIdAndSenderType(Long caseId, Long senderId, String senderType);
    List<Message> findByCaseIdAndReceiverIdAndReceiverType(Long caseId, Long receiverId, String receiverType);
    long countByReceiverIdAndReceiverTypeAndIsRead(Long receiverId, String receiverType, Boolean isRead);
}

