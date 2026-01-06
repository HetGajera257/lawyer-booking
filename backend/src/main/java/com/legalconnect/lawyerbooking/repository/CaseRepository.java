package com.legalconnect.lawyerbooking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.legalconnect.lawyerbooking.entity.Case;
import java.util.List;

@Repository
public interface CaseRepository extends JpaRepository<Case, Long> {
    List<Case> findByUserId(Long userId);
    List<Case> findByLawyerId(Long lawyerId);
    List<Case> findByCaseStatus(String status);
    List<Case> findByLawyerIdIsNull(); // Cases not yet assigned to a lawyer
}

