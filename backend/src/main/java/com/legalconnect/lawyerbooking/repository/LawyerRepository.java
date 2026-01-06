package com.legalconnect.lawyerbooking.repository;

import com.legalconnect.lawyerbooking.entity.Lawyer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LawyerRepository extends JpaRepository<Lawyer, Long> {
    Optional<Lawyer> findByUsername(String username);
    Optional<Lawyer> findByUsernameAndPassword(String username, String password);
    boolean existsByUsername(String username);
}

