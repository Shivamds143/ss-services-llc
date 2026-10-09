package com.financialcrm.repository;

import com.financialcrm.entity.IncomeEntry;
import com.financialcrm.entity.IncomeEntry.PaymentMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for IncomeEntry.
 * Implements JpaSpecificationExecutor for dynamic multi-filter queries.
 */
@Repository
public interface IncomeEntryRepository
        extends JpaRepository<IncomeEntry, Long>, JpaSpecificationExecutor<IncomeEntry> {

    // ── Paginated list with FETCH (avoids N+1 on list view) ───────────────
    @Query(value = "SELECT ie FROM IncomeEntry ie " +
                   "LEFT JOIN FETCH ie.incomeCategory " +
                   "LEFT JOIN FETCH ie.customer " +
                   "LEFT JOIN FETCH ie.employee " +
                   "LEFT JOIN FETCH ie.createdBy " +
                   "ORDER BY ie.incomeDate DESC, ie.id DESC",
           countQuery = "SELECT COUNT(ie) FROM IncomeEntry ie")
    Page<IncomeEntry> findAllWithDetails(Pageable pageable);

    // ── Single record with FETCH (edit/view) ──────────────────────────────
    @Query("SELECT ie FROM IncomeEntry ie " +
           "LEFT JOIN FETCH ie.incomeCategory " +
           "LEFT JOIN FETCH ie.customer " +
           "LEFT JOIN FETCH ie.employee " +
           "LEFT JOIN FETCH ie.createdBy " +
           "WHERE ie.id = :id")
    Optional<IncomeEntry> findByIdWithDetails(@Param("id") Long id);

    // ── Date range ─────────────────────────────────────────────────────────
    @Query("SELECT ie FROM IncomeEntry ie " +
           "LEFT JOIN FETCH ie.incomeCategory " +
           "LEFT JOIN FETCH ie.customer " +
           "LEFT JOIN FETCH ie.employee " +
           "WHERE ie.incomeDate BETWEEN :startDate AND :endDate " +
           "ORDER BY ie.incomeDate DESC, ie.id DESC")
    List<IncomeEntry> findByDateRange(@Param("startDate") LocalDate startDate,
                                      @Param("endDate")   LocalDate endDate);

    // ── Filter by category ─────────────────────────────────────────────────
    @Query(value = "SELECT ie FROM IncomeEntry ie " +
                   "LEFT JOIN FETCH ie.incomeCategory " +
                   "LEFT JOIN FETCH ie.customer " +
                   "LEFT JOIN FETCH ie.employee " +
                   "WHERE ie.incomeCategory.id = :categoryId " +
                   "ORDER BY ie.incomeDate DESC",
           countQuery = "SELECT COUNT(ie) FROM IncomeEntry ie WHERE ie.incomeCategory.id = :categoryId")
    Page<IncomeEntry> findByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);

    // ── Filter by employee ─────────────────────────────────────────────────
    @Query(value = "SELECT ie FROM IncomeEntry ie " +
                   "LEFT JOIN FETCH ie.incomeCategory " +
                   "LEFT JOIN FETCH ie.customer " +
                   "LEFT JOIN FETCH ie.employee " +
                   "WHERE ie.employee.id = :employeeId " +
                   "ORDER BY ie.incomeDate DESC",
           countQuery = "SELECT COUNT(ie) FROM IncomeEntry ie WHERE ie.employee.id = :employeeId")
    Page<IncomeEntry> findByEmployeeId(@Param("employeeId") Long employeeId, Pageable pageable);

    // ── Filter by payment mode ─────────────────────────────────────────────
    @Query(value = "SELECT ie FROM IncomeEntry ie " +
                   "LEFT JOIN FETCH ie.incomeCategory " +
                   "LEFT JOIN FETCH ie.customer " +
                   "LEFT JOIN FETCH ie.employee " +
                   "WHERE ie.paymentMode = :paymentMode " +
                   "ORDER BY ie.incomeDate DESC",
           countQuery = "SELECT COUNT(ie) FROM IncomeEntry ie WHERE ie.paymentMode = :paymentMode")
    Page<IncomeEntry> findByPaymentMode(@Param("paymentMode") PaymentMode paymentMode, Pageable pageable);

    // ── Aggregations ───────────────────────────────────────────────────────
    @Query("SELECT SUM(ie.amount) FROM IncomeEntry ie " +
           "WHERE ie.incomeDate BETWEEN :startDate AND :endDate")
    BigDecimal sumByDateRange(@Param("startDate") LocalDate startDate,
                               @Param("endDate")   LocalDate endDate);

    @Query("SELECT SUM(ie.amount) FROM IncomeEntry ie " +
           "WHERE MONTH(ie.incomeDate) = MONTH(CURRENT_DATE) " +
           "AND YEAR(ie.incomeDate) = YEAR(CURRENT_DATE)")
    BigDecimal getCurrentMonthTotal();

    @Query("SELECT MONTH(ie.incomeDate), SUM(ie.amount) " +
           "FROM IncomeEntry ie " +
           "WHERE YEAR(ie.incomeDate) = :year " +
           "GROUP BY MONTH(ie.incomeDate) " +
           "ORDER BY MONTH(ie.incomeDate)")
    List<Object[]> getMonthlyTotals(@Param("year") int year);

    @Query("SELECT ie.incomeCategory.categoryName, SUM(ie.amount) " +
           "FROM IncomeEntry ie " +
           "WHERE MONTH(ie.incomeDate) = :month AND YEAR(ie.incomeDate) = :year " +
           "GROUP BY ie.incomeCategory.categoryName " +
           "ORDER BY SUM(ie.amount) DESC")
    List<Object[]> getMonthlyCategoryWise(@Param("month") int month,
                                           @Param("year")  int year);

    @Query("SELECT ie.incomeCategory.categoryGroup, SUM(ie.amount) " +
           "FROM IncomeEntry ie " +
           "WHERE ie.incomeDate BETWEEN :startDate AND :endDate " +
           "GROUP BY ie.incomeCategory.categoryGroup " +
           "ORDER BY SUM(ie.amount) DESC")
    List<Object[]> sumByGroupAndDateRange(@Param("startDate") LocalDate startDate,
                                           @Param("endDate")   LocalDate endDate);

    @Query("SELECT ie.employee.employeeName, SUM(ie.amount) " +
           "FROM IncomeEntry ie " +
           "WHERE ie.employee IS NOT NULL " +
           "AND ie.incomeDate BETWEEN :startDate AND :endDate " +
           "GROUP BY ie.employee.employeeName " +
           "ORDER BY SUM(ie.amount) DESC")
    List<Object[]> sumByEmployeeAndDateRange(@Param("startDate") LocalDate startDate,
                                              @Param("endDate")   LocalDate endDate);

    // ── Customer-linked entries ────────────────────────────────────────────
    List<IncomeEntry> findByCustomerIdOrderByIncomeDateDesc(Long customerId);

    // ── Count for dashboard badges ─────────────────────────────────────────
    @Query("SELECT COUNT(ie) FROM IncomeEntry ie " +
           "WHERE MONTH(ie.incomeDate) = MONTH(CURRENT_DATE) " +
           "AND YEAR(ie.incomeDate) = YEAR(CURRENT_DATE)")
    long countCurrentMonth();

    // ── Dashboard Graph 2: Insurance Income by type ────────────────────────
    // Returns [insuranceType, totalAmount] for Life / Health / General buckets.
    @Query(value = "SELECT " +
                   "  MONTH(ie.income_date), " +
                   "  CASE " +
                   "    WHEN LOWER(ic.category_name) LIKE '%life%' THEN 'Life Insurance' " +
                   "    WHEN LOWER(ic.category_name) LIKE '%health%' THEN 'Health Insurance' " +
                   "    WHEN LOWER(ic.category_name) LIKE '%general%' THEN 'General Insurance' " +
                   "    ELSE 'General Insurance' " +
                   "  END AS insurance_type, " +
                   "  SUM(ie.amount) " +
                   "FROM income_entries ie " +
                   "JOIN income_categories ic ON ie.income_category_id = ic.id " +
                   "WHERE YEAR(ie.income_date) = :year " +
                   "AND (LOWER(ic.category_group) LIKE '%insurance%' " +
                   "   OR LOWER(ic.category_name) LIKE '%insurance%' " +
                   ") " +
                   "GROUP BY MONTH(ie.income_date), insurance_type " +
                   "ORDER BY MONTH(ie.income_date), FIELD(insurance_type, 'Life Insurance', 'Health Insurance', 'General Insurance'), insurance_type",
           nativeQuery = true)
    List<Object[]> sumMonthlyByInsuranceTypes(@Param("year") int year);

    // ── Dashboard Graph 3: Mutual Fund income total ────────────────────────
    // Returns [categoryGroup, totalAmount] for mutual fund / SIP groups
    @Query(value = "SELECT MONTH(ie.income_date), ic.category_name, SUM(ie.amount) " +
                   "FROM income_entries ie " +
                   "JOIN income_categories ic ON ie.income_category_id = ic.id " +
                   "WHERE YEAR(ie.income_date) = :year " +
                   "AND (LOWER(ic.category_group) LIKE '%mutual%' " +
                   "   OR LOWER(ic.category_group) LIKE '%sip%' " +
                   "   OR LOWER(ic.category_group) LIKE '%fund%' " +
                   "   OR LOWER(ic.category_name) LIKE '%mutual%' " +
                   "   OR LOWER(ic.category_name) LIKE '%sip%' " +
                   "   OR LOWER(ic.category_name) LIKE '%fund%' " +
                   ") " +
                   "GROUP BY MONTH(ie.income_date), ic.category_name " +
                   "ORDER BY MONTH(ie.income_date), ic.category_name",
           nativeQuery = true)
    List<Object[]> sumMonthlyByMutualFundCategories(@Param("year") int year);

    // ── All category groups with total income (for graph fallback) ─────────
    @Query("SELECT ie.incomeCategory.categoryGroup, SUM(ie.amount) " +
           "FROM IncomeEntry ie " +
           "GROUP BY ie.incomeCategory.categoryGroup " +
           "ORDER BY SUM(ie.amount) DESC")
    List<Object[]> sumByAllCategoryGroups();
}
