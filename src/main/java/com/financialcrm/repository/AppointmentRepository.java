package com.financialcrm.repository;

import com.financialcrm.entity.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query("SELECT a FROM Appointment a " +
           "LEFT JOIN FETCH a.customer " +
           "LEFT JOIN FETCH a.assignedTo " +
           "WHERE a.appointmentDate = :date " +
           "ORDER BY a.appointmentTime ASC")
    List<Appointment> findByDate(@Param("date") LocalDate date);

    @Query("SELECT a FROM Appointment a " +
           "LEFT JOIN FETCH a.customer " +
           "LEFT JOIN FETCH a.assignedTo " +
           "WHERE a.appointmentDate BETWEEN :startDate AND :endDate " +
           "ORDER BY a.appointmentDate ASC, a.appointmentTime ASC")
    List<Appointment> findByDateRange(@Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate);

    @Query("SELECT a FROM Appointment a " +
           "LEFT JOIN FETCH a.customer " +
           "WHERE a.customer.id = :customerId " +
           "ORDER BY a.appointmentDate DESC")
    List<Appointment> findByCustomerIdOrderByAppointmentDateDesc(@Param("customerId") Long customerId);

    @Query("SELECT a FROM Appointment a " +
           "LEFT JOIN FETCH a.customer " +
           "LEFT JOIN FETCH a.assignedTo " +
           "WHERE a.assignedTo.id = :userId " +
           "AND a.appointmentDate >= :today " +
           "ORDER BY a.appointmentDate ASC")
    List<Appointment> findUpcomingByAssignedTo(@Param("userId") Long userId,
                                               @Param("today") LocalDate today);

    Page<Appointment> findByOrderByAppointmentDateDescAppointmentTimeDesc(Pageable pageable);

    long countByAppointmentDate(LocalDate date);
}