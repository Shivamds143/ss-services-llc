package com.financialcrm.service.impl;

import com.financialcrm.entity.Employee;
import com.financialcrm.repository.EmployeeRepository;
import com.financialcrm.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;

    @Value("${app.employee.photo.dir:uploads/employees/}")
    private String photoDir;

    @Override
    public Employee save(Employee employee, MultipartFile photo) throws IOException {
        if (photo != null && !photo.isEmpty()) {
            String originalFilename = photo.getOriginalFilename();
            String cleanName = StringUtils.hasText(originalFilename)
                    ? StringUtils.cleanPath(originalFilename)
                    : "photo.jpg";
            String filename = UUID.randomUUID() + "_" + cleanName;

            Path uploadPath = Paths.get(photoDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            Files.copy(photo.getInputStream(), uploadPath.resolve(filename),
                    StandardCopyOption.REPLACE_EXISTING);
            employee.setPhotoPath(photoDir + filename);
            log.info("Employee photo saved: {}", filename);
        }
        return employeeRepository.save(employee);
    }

    @Override
    public Employee save(Employee employee) {
        return employeeRepository.save(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Employee> findById(Long id) {
        return employeeRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Employee> findByIdWithUser(Long id) {
        return employeeRepository.findByIdWithUser(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Employee> findAll(Pageable pageable) {
        return employeeRepository.findByIsActiveTrueOrderByEmployeeNameAsc(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> findAll() {
        return employeeRepository.findByIsActiveTrueOrderByEmployeeNameAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Employee> search(String query, Pageable pageable) {
        if (!StringUtils.hasText(query)) return findAll(pageable);
        return employeeRepository.searchEmployees(query, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> findAllActive() {
        return employeeRepository.findByIsActiveTrueOrderByEmployeeNameAsc();
    }

    // FIX: direct lookup by user_id — avoids scanning all employees
    @Override
    @Transactional(readOnly = true)
    public Optional<Employee> findByUserId(Long userId) {
        return employeeRepository.findByUserId(userId);
    }

    @Override
    public void deactivate(Long id) {
        employeeRepository.findById(id).ifPresent(e -> {
            e.setIsActive(false);
            employeeRepository.save(e);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public String generateNextEmployeeId() {
        Integer max = employeeRepository.findMaxEmployeeNumber();
        int next = (max == null ? 0 : max) + 1;
        return String.format("EMP%03d", next);
    }

    @Override
    @Transactional(readOnly = true)
    public long countActive() {
        return employeeRepository.countByIsActiveTrue();
    }
}