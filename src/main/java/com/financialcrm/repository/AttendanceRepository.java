package com.financialcrm.repository;

import com.financialcrm.entity.Attendance;
import com.financialcrm.entity.Attendance.AttendanceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByEmployeeIdAndAttendanceDate(Long employeeId, LocalDate date);

    // Eager fetch employee inside transaction — fixes lazy load blank columns
    @Query("SELECT a FROM Attendance a " +
           "LEFT JOIN FETCH a.employee " +
           "WHERE a.attendanceDate = :date " +
           "ORDER BY a.employee.employeeName ASC")
    List<Attendance> findByDateWithEmployee(@Param("date") LocalDate date);

    @Query("SELECT a FROM Attendance a " +
           "LEFT JOIN FETCH a.employee " +
           "WHERE a.employee.id = :empId " +
           "AND a.attendanceDate BETWEEN :startDate AND :endDate " +
           "ORDER BY a.attendanceDate ASC")
    List<Attendance> findByEmployeeAndDateRange(@Param("empId") Long empId,
                                                @Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);

    @Query("SELECT a.attendanceType, COUNT(a) FROM Attendance a " +
           "WHERE a.employee.id = :empId " +
           "AND MONTH(a.attendanceDate) = :month " +
           "AND YEAR(a.attendanceDate) = :year " +
           "GROUP BY a.attendanceType")
    List<Object[]> getMonthlyAttendanceSummary(@Param("empId") Long empId,
                                               @Param("month") int month,
                                               @Param("year") int year);

    @Query("SELECT a.attendanceDate, " +
           "COUNT(CASE WHEN a.attendanceType = 'PRESENT' THEN 1 END), " +
           "COUNT(CASE WHEN a.attendanceType = 'ABSENT' THEN 1 END) " +
           "FROM Attendance a " +
           "WHERE a.attendanceDate = :date " +
           "GROUP BY a.attendanceDate")
    Object[] getDailyAttendanceSummary(@Param("date") LocalDate date);

    long countByAttendanceDateAndAttendanceType(LocalDate date, AttendanceType type);
}