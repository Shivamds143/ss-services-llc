package com.financialcrm.repository;

import com.financialcrm.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmployeeId(String employeeId);
    Page<Employee> findByIsActiveTrueOrderByEmployeeNameAsc(Pageable pageable);
    List<Employee> findByIsActiveTrueOrderByEmployeeNameAsc();

    // FIX: find employee linked to a specific user id (used in my-work page)
    Optional<Employee> findByUserId(Long userId);

    @Query("SELECT e FROM Employee e WHERE e.isActive = true AND " +
           "(LOWER(e.employeeName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "e.mobileNumber LIKE CONCAT('%', :search, '%') OR " +
           "LOWER(e.designation) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Employee> searchEmployees(@Param("search") String search, Pageable pageable);

    boolean existsByEmployeeId(String employeeId);

    @Query("SELECT MAX(CAST(SUBSTRING(e.employeeId, 4) AS int)) FROM Employee e WHERE e.employeeId LIKE 'EMP%'")
    Integer findMaxEmployeeNumber();

    long countByIsActiveTrue();
    
    @Query("SELECT e FROM Employee e LEFT JOIN FETCH e.user WHERE e.id = :id")
    Optional<Employee> findByIdWithUser(@Param("id") Long id);
}