package com.financialcrm.service;

import com.financialcrm.entity.Attendance;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceService {
    Attendance save(Attendance attendance);
    Optional<Attendance> findById(Long id);
    Optional<Attendance> findByEmployeeAndDate(Long employeeId, LocalDate date);
    List<Attendance> findByDate(LocalDate date);
    List<Attendance> findByEmployeeAndDateRange(Long employeeId, LocalDate start, LocalDate end);
    List<Object[]> getMonthlyAttendanceSummary(Long empId, int month, int year);
    long countByDateAndType(LocalDate date, Attendance.AttendanceType type);
    void deleteById(Long id);
}
