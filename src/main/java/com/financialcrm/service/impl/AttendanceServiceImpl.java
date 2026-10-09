package com.financialcrm.service.impl;

import com.financialcrm.entity.Attendance;
import com.financialcrm.repository.AttendanceRepository;
import com.financialcrm.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;

    @Override
    public Attendance save(Attendance attendance) {
        return attendanceRepository.save(attendance);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Attendance> findById(Long id) {
        return attendanceRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Attendance> findByEmployeeAndDate(Long employeeId, LocalDate date) {
        return attendanceRepository.findByEmployeeIdAndAttendanceDate(employeeId, date);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Attendance> findByDate(LocalDate date) {
        // Uses eager JOIN FETCH so employee is loaded inside transaction
        return attendanceRepository.findByDateWithEmployee(date);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Attendance> findByEmployeeAndDateRange(Long empId, LocalDate start, LocalDate end) {
        return attendanceRepository.findByEmployeeAndDateRange(empId, start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> getMonthlyAttendanceSummary(Long empId, int month, int year) {
        return attendanceRepository.getMonthlyAttendanceSummary(empId, month, year);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByDateAndType(LocalDate date, Attendance.AttendanceType type) {
        return attendanceRepository.countByAttendanceDateAndAttendanceType(date, type);
    }

    @Override
    public void deleteById(Long id) {
        attendanceRepository.deleteById(id);
    }
}