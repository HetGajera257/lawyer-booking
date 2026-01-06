package com.legalconnect.lawyerbooking.repository;

import com.legalconnect.lawyerbooking.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    
    // Find all appointments for a user
    List<Appointment> findByUserIdOrderByAppointmentDateDesc(Long userId);
    
    // Find all appointments for a lawyer
    List<Appointment> findByLawyerIdOrderByAppointmentDateDesc(Long lawyerId);
    
    // Find appointments by status
    List<Appointment> findByStatusOrderByAppointmentDateAsc(String status);
    
    // Find appointments by user and status
    List<Appointment> findByUserIdAndStatusOrderByAppointmentDateAsc(Long userId, String status);
    
    // Find appointments by lawyer and status
    List<Appointment> findByLawyerIdAndStatusOrderByAppointmentDateAsc(Long lawyerId, String status);
    
    // Check for overlapping appointments for a lawyer
    // Using native query for MySQL DATE_ADD function
    @Query(value = "SELECT * FROM appointments WHERE lawyer_id = :lawyerId " +
           "AND status != 'cancelled' " +
           "AND appointment_date < :endTime " +
           "AND DATE_ADD(appointment_date, INTERVAL duration_minutes MINUTE) > :startTime", 
           nativeQuery = true)
    List<Appointment> findOverlappingAppointments(
        @Param("lawyerId") Long lawyerId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
    
    // Find upcoming appointments for a user
    @Query("SELECT a FROM Appointment a WHERE a.userId = :userId " +
           "AND a.appointmentDate >= :now " +
           "AND a.status != 'cancelled' " +
           "ORDER BY a.appointmentDate ASC")
    List<Appointment> findUpcomingByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);
    
    // Find upcoming appointments for a lawyer
    @Query("SELECT a FROM Appointment a WHERE a.lawyerId = :lawyerId " +
           "AND a.appointmentDate >= :now " +
           "AND a.status != 'cancelled' " +
           "ORDER BY a.appointmentDate ASC")
    List<Appointment> findUpcomingByLawyerId(@Param("lawyerId") Long lawyerId, @Param("now") LocalDateTime now);
}

