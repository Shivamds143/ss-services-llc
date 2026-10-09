package com.financialcrm.repository;

import com.financialcrm.entity.EmployeeDailyWork;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeDailyWorkRepository extends JpaRepository<EmployeeDailyWork, Long> {

    Optional<EmployeeDailyWork> findByEmployeeIdAndWorkDate(Long employeeId, LocalDate workDate);

    @Query("SELECT w FROM EmployeeDailyWork w WHERE w.employee.id = :employeeId " +
           "AND w.workDate = :workDate AND w.deleted = false")
    Optional<EmployeeDailyWork> findVisibleByEmployeeIdAndWorkDate(@Param("employeeId") Long employeeId,
                                                                   @Param("workDate") LocalDate workDate);

    List<EmployeeDailyWork> findByEmployeeIdOrderByWorkDateDesc(Long employeeId);

    @Query("SELECT w FROM EmployeeDailyWork w WHERE w.employee.id = :empId " +
           "AND w.workDate BETWEEN :start AND :end AND (:includeDeleted = true OR w.deleted = false) " +
           "ORDER BY w.workDate DESC")
    List<EmployeeDailyWork> findByEmployeeAndDateRange(@Param("empId") Long empId,
                                                        @Param("start") LocalDate start,
                                                        @Param("end") LocalDate end,
                                                        @Param("includeDeleted") boolean includeDeleted);

    @Query("SELECT w FROM EmployeeDailyWork w JOIN FETCH w.employee " +
           "WHERE w.workDate = :date AND (:includeDeleted = true OR w.deleted = false) " +
           "ORDER BY w.employee.employeeName ASC")
    List<EmployeeDailyWork> findAllByWorkDate(@Param("date") LocalDate date,
                                              @Param("includeDeleted") boolean includeDeleted);

    // Monthly totals for a single employee
    @Query("SELECT COALESCE(SUM(w.dailyLeads),0), COALESCE(SUM(w.totalCalls),0), " +
           "COALESCE(SUM(w.connectedCalls),0) " +
           "FROM EmployeeDailyWork w WHERE w.employee.id = :empId " +
           "AND MONTH(w.workDate) = :month AND YEAR(w.workDate) = :year AND w.deleted = false")
    Object[] getMonthlyTotals(@Param("empId") Long empId,
                               @Param("month") int month,
                               @Param("year") int year);

    // Team totals for a given date
    @Query("SELECT COALESCE(SUM(w.dailyLeads),0), COALESCE(SUM(w.totalCalls),0), " +
           "COALESCE(SUM(w.connectedCalls),0) FROM EmployeeDailyWork w " +
           "WHERE w.workDate = :date AND w.deleted = false")
    Object[] getTeamTotalsForDate(@Param("date") LocalDate date);

    List<EmployeeDailyWork> findByDeletedFalseOrderByWorkDateDesc();

    @Query("SELECT w FROM EmployeeDailyWork w WHERE w.employee.id IN :employeeIds " +
           "AND w.workDate BETWEEN :start AND :end AND w.deleted = false ORDER BY w.workDate ASC")
    List<EmployeeDailyWork> findVisibleByEmployeeIdsAndRange(@Param("employeeIds") Collection<Long> employeeIds,
                                                             @Param("start") LocalDate start,
                                                             @Param("end") LocalDate end);
}
