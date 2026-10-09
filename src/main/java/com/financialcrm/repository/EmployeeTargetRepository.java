package com.financialcrm.repository;

import com.financialcrm.entity.EmployeeTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeTargetRepository extends JpaRepository<EmployeeTarget, Long> {

    Optional<EmployeeTarget> findByEmployeeIdAndTargetMonthAndTargetYear(
            Long employeeId, int month, int year);

    List<EmployeeTarget> findByTargetMonthAndTargetYearOrderByEmployee_EmployeeNameAsc(
            int month, int year);

    List<EmployeeTarget> findByEmployeeIdOrderByTargetYearDescTargetMonthDesc(Long employeeId);
}