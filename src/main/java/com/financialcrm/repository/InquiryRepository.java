package com.financialcrm.repository;

import com.financialcrm.entity.Inquiry;
import com.financialcrm.entity.Inquiry.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    @Query(value = "SELECT i FROM Inquiry i " +
           "LEFT JOIN FETCH i.customer " +
           "LEFT JOIN FETCH i.inquiryType " +
           "LEFT JOIN FETCH i.assignedTo " +
           "LEFT JOIN FETCH i.createdBy",
           countQuery = "SELECT COUNT(i) FROM Inquiry i")
    Page<Inquiry> findByOrderByCreatedAtDesc(Pageable pageable);

    @Query(value = "SELECT i FROM Inquiry i " +
                   "LEFT JOIN FETCH i.inquiryType " +
                   "WHERE i.customer.id = :customerId " +
                   "ORDER BY i.createdAt DESC")
    List<Inquiry> findByCustomerId(@Param("customerId") Long customerId);

    @Query(value = "SELECT i FROM Inquiry i " +
           "LEFT JOIN FETCH i.customer c " +
           "LEFT JOIN FETCH i.inquiryType " +
           "LEFT JOIN FETCH i.assignedTo " +
           "LEFT JOIN FETCH i.createdBy " +
           "WHERE i.status = :status",
           countQuery = "SELECT COUNT(i) FROM Inquiry i WHERE i.status = :status")
    Page<Inquiry> findByStatusOrderByCreatedAtDesc(@Param("status") Status status, Pageable pageable);

    @Query(value = "SELECT i FROM Inquiry i " +
           "LEFT JOIN FETCH i.customer c " +
           "LEFT JOIN FETCH i.inquiryType " +
           "LEFT JOIN FETCH i.assignedTo " +
           "LEFT JOIN FETCH i.createdBy " +
           "WHERE (LOWER(c.customerName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "c.mobileNumber LIKE CONCAT('%', :search, '%') OR " +
           "LOWER(i.bankName) LIKE LOWER(CONCAT('%', :search, '%')))",
           countQuery = "SELECT COUNT(i) FROM Inquiry i " +
           "LEFT JOIN i.customer c " +
           "WHERE (LOWER(c.customerName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "c.mobileNumber LIKE CONCAT('%', :search, '%') OR " +
           "LOWER(i.bankName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Inquiry> searchInquiries(@Param("search") String search, Pageable pageable);

    @Query(value = "SELECT i FROM Inquiry i " +
           "LEFT JOIN FETCH i.customer c " +
           "LEFT JOIN FETCH i.inquiryType " +
           "LEFT JOIN FETCH i.assignedTo " +
           "LEFT JOIN FETCH i.createdBy " +
           "WHERE i.status = :status AND " +
           "(LOWER(c.customerName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "c.mobileNumber LIKE CONCAT('%', :search, '%') OR " +
           "LOWER(i.bankName) LIKE LOWER(CONCAT('%', :search, '%')))",
           countQuery = "SELECT COUNT(i) FROM Inquiry i " +
           "LEFT JOIN i.customer c " +
           "WHERE i.status = :status AND " +
           "(LOWER(c.customerName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "c.mobileNumber LIKE CONCAT('%', :search, '%') OR " +
           "LOWER(i.bankName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Inquiry> searchInquiriesByStatus(@Param("search") String search,
                                          @Param("status") Status status,
                                          Pageable pageable);

    // NEW: today filter — paginated, combined with optional search + status
    @Query(value = "SELECT i FROM Inquiry i " +
           "LEFT JOIN FETCH i.customer c " +
           "LEFT JOIN FETCH i.inquiryType " +
           "LEFT JOIN FETCH i.assignedTo " +
           "LEFT JOIN FETCH i.createdBy " +
           "WHERE i.inquiryDate = :today",
           countQuery = "SELECT COUNT(i) FROM Inquiry i WHERE i.inquiryDate = :today")
    Page<Inquiry> findTodayInquiries(@Param("today") LocalDate today, Pageable pageable);

    @Query(value = "SELECT i FROM Inquiry i " +
           "LEFT JOIN FETCH i.customer c " +
           "LEFT JOIN FETCH i.inquiryType " +
           "LEFT JOIN FETCH i.assignedTo " +
           "LEFT JOIN FETCH i.createdBy " +
           "WHERE i.inquiryDate = :today AND i.status = :status",
           countQuery = "SELECT COUNT(i) FROM Inquiry i WHERE i.inquiryDate = :today AND i.status = :status")
    Page<Inquiry> findTodayInquiriesByStatus(@Param("today") LocalDate today,
                                             @Param("status") Status status,
                                             Pageable pageable);

    @Query(value = "SELECT i FROM Inquiry i " +
           "LEFT JOIN FETCH i.customer c " +
           "LEFT JOIN FETCH i.inquiryType " +
           "LEFT JOIN FETCH i.assignedTo " +
           "LEFT JOIN FETCH i.createdBy " +
           "WHERE i.inquiryDate = :today AND " +
           "(LOWER(c.customerName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "c.mobileNumber LIKE CONCAT('%', :search, '%') OR " +
           "LOWER(i.bankName) LIKE LOWER(CONCAT('%', :search, '%')))",
           countQuery = "SELECT COUNT(i) FROM Inquiry i LEFT JOIN i.customer c " +
           "WHERE i.inquiryDate = :today AND " +
           "(LOWER(c.customerName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "c.mobileNumber LIKE CONCAT('%', :search, '%') OR " +
           "LOWER(i.bankName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Inquiry> searchTodayInquiries(@Param("today") LocalDate today,
                                       @Param("search") String search,
                                       Pageable pageable);

    @Query(value = "SELECT i FROM Inquiry i " +
           "LEFT JOIN FETCH i.customer c " +
           "LEFT JOIN FETCH i.inquiryType " +
           "LEFT JOIN FETCH i.assignedTo " +
           "LEFT JOIN FETCH i.createdBy " +
           "WHERE i.inquiryDate = :today AND i.status = :status AND " +
           "(LOWER(c.customerName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "c.mobileNumber LIKE CONCAT('%', :search, '%') OR " +
           "LOWER(i.bankName) LIKE LOWER(CONCAT('%', :search, '%')))",
           countQuery = "SELECT COUNT(i) FROM Inquiry i LEFT JOIN i.customer c " +
           "WHERE i.inquiryDate = :today AND i.status = :status AND " +
           "(LOWER(c.customerName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "c.mobileNumber LIKE CONCAT('%', :search, '%') OR " +
           "LOWER(i.bankName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Inquiry> searchTodayInquiriesByStatus(@Param("today") LocalDate today,
                                               @Param("search") String search,
                                               @Param("status") Status status,
                                               Pageable pageable);

    @Query("SELECT i FROM Inquiry i " +
           "LEFT JOIN FETCH i.customer " +
           "LEFT JOIN FETCH i.inquiryType " +
           "LEFT JOIN FETCH i.assignedTo " +
           "LEFT JOIN FETCH i.createdBy " +
           "LEFT JOIN FETCH i.followups f " +
           "WHERE i.id = :id")
    Optional<Inquiry> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT COUNT(i) FROM Inquiry i WHERE i.status = :status")
    long countByStatus(@Param("status") Status status);

    @Query("SELECT COUNT(i) FROM Inquiry i")
    long countTotalLeads();

    @Query("SELECT i FROM Inquiry i WHERE i.assignedTo.id = :userId ORDER BY i.createdAt DESC")
    List<Inquiry> findByAssignedToId(@Param("userId") Long userId);

    @Query("SELECT i FROM Inquiry i WHERE i.inquiryDate BETWEEN :startDate AND :endDate ORDER BY i.inquiryDate DESC")
    List<Inquiry> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT i.inquiryType.typeName, COUNT(i) FROM Inquiry i GROUP BY i.inquiryType.typeName")
    List<Object[]> countByInquiryType();

    @Query("SELECT i.status, COUNT(i) FROM Inquiry i GROUP BY i.status")
    List<Object[]> countByStatusGrouped();

    @Query("SELECT MONTH(i.inquiryDate), COUNT(i) FROM Inquiry i WHERE YEAR(i.inquiryDate) = :year GROUP BY MONTH(i.inquiryDate)")
    List<Object[]> countMonthlyByYear(@Param("year") int year);

    // NEW: count today's inquiries for badge display
    @Query("SELECT COUNT(i) FROM Inquiry i WHERE i.inquiryDate = :today")
    long countTodayInquiries(@Param("today") LocalDate today);

    // ── Dashboard Graph 1: Loan Income — Sanctioned vs Rejected ──────────
    // Returns [status, totalLoanAmount, count] for APPROVED and REJECTED loan-type inquiries
    @Query("SELECT MONTH(i.inquiryDate), i.status, SUM(COALESCE(i.loanAmount, 0)), COUNT(i) " +
           "FROM Inquiry i " +
           "WHERE YEAR(i.inquiryDate) = :year " +
           "AND i.status IN ('APPROVED', 'REJECTED') " +
           "AND i.inquiryType.typeCode NOT IN ('INSURANCE', 'MUTUAL_FUND', 'CIBIL_CHECK', 'VISIT_FOR_SIR') " +
           "GROUP BY MONTH(i.inquiryDate), i.status " +
           "ORDER BY MONTH(i.inquiryDate), i.status")
    List<Object[]> sumMonthlyLoanAmountByApprovedRejected(@Param("year") int year);

    // Count all inquiries by status for loan types (for dashboard overview)
    @Query("SELECT i.status, COUNT(i) " +
           "FROM Inquiry i " +
           "WHERE i.inquiryType.typeCode NOT IN ('INSURANCE', 'MUTUAL_FUND', 'CIBIL_CHECK', 'VISIT_FOR_SIR') " +
           "GROUP BY i.status")
    List<Object[]> countLoanInquiriesByStatus();
}
