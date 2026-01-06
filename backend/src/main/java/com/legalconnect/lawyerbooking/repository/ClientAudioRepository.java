package com.legalconnect.lawyerbooking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.legalconnect.lawyerbooking.entity.ClientAudio;
import java.util.List;

public interface ClientAudioRepository extends JpaRepository<ClientAudio, Long> {
    List<ClientAudio> findByUserId(Long userId);
    List<ClientAudio> findByCaseId(Long caseId);
    List<ClientAudio> findByLawyerId(Long lawyerId);
}

