package com.legalconnect.lawyerbooking.repository;

import com.legalconnect.lawyerbooking.entity.LawyerAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LawyerAvailabilityRepository extends JpaRepository<LawyerAvailability, Long> {
    
    List<LawyerAvailability> findByLawyerId(Long lawyerId);
    
    List<LawyerAvailability> findByLawyerIdAndIsAvailableTrue(Long lawyerId);
    
    Optional<LawyerAvailability> findByLawyerIdAndDayOfWeek(Long lawyerId, Integer dayOfWeek);
    
    void deleteByLawyerId(Long lawyerId);
}

