package com.financialcrm.repository;

import com.financialcrm.entity.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long> {

    /** All active top-level categories */
    List<ExpenseCategory> findByIsActiveTrueAndParentIsNullOrderBySortOrderAsc();

    /** All active categories (top-level + sub) */
    List<ExpenseCategory> findByIsActiveTrueOrderBySortOrderAsc();

    /** Sub-categories of a specific parent */
    @Query("SELECT c FROM ExpenseCategory c WHERE c.parent.id = :parentId AND c.isActive = true ORDER BY c.sortOrder ASC")
    List<ExpenseCategory> findActiveSubCategoriesByParentId(@Param("parentId") Long parentId);

    /** Used by the category seeder to avoid duplicate-name violations */
    Optional<ExpenseCategory> findByCategoryName(String categoryName);

    Optional<ExpenseCategory> findByCategoryCode(String categoryCode);
}