package com.financialcrm.service;

import com.financialcrm.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface EmployeeService {
    Employee save(Employee employee, MultipartFile photo) throws IOException;
    Employee save(Employee employee);
    Optional<Employee> findById(Long id);
    Optional<Employee> findByIdWithUser(Long id);
    Page<Employee> findAll(Pageable pageable);
    List<Employee> findAll();
    Page<Employee> search(String query, Pageable pageable);
    List<Employee> findAllActive();
    // FIX: find the employee profile linked to a given user id
    Optional<Employee> findByUserId(Long userId);
    void deactivate(Long id);
    String generateNextEmployeeId();
    long countActive();
    
}