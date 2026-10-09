package com.financialcrm.repository;

import com.financialcrm.entity.IncomeCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncomeCategoryRepository extends JpaRepository<IncomeCategory, Long> {

    List<IncomeCategory> findByActiveTrueOrderByCategoryGroupAscCategoryNameAsc();

    List<IncomeCategory> findAllByOrderByCategoryGroupAscCategoryNameAsc();

    List<IncomeCategory> findByCategoryGroupAndActiveTrueOrderByCategoryNameAsc(String categoryGroup);

    // Called by IncomeCategoryController.list()
    @Query("SELECT DISTINCT ic.categoryGroup FROM IncomeCategory ic WHERE ic.active = true ORDER BY ic.categoryGroup")
    List<String> findDistinctCategoryGroups();

    // Called by IncomeCategoryController.update()
    Optional<IncomeCategory> findByCategoryNameIgnoreCase(String categoryName);

    boolean existsByCategoryNameIgnoreCase(String categoryName);

    boolean existsByCategoryNameIgnoreCaseAndIdNot(String categoryName, Long id);

    // Replaces broken findByIsActiveTrueOrderBySortOrderAsc()
    // IncomeCategory uses field "active" (not "isActive") and has no "sortOrder"
    List<IncomeCategory> findByActiveTrueOrderByCategoryNameAsc();

    // Replaces broken findDistinctActiveGroups() — delegates to findDistinctCategoryGroups()
    @Query("SELECT DISTINCT ic.categoryGroup FROM IncomeCategory ic WHERE ic.active = true ORDER BY ic.categoryGroup")
    List<String> findDistinctActiveGroups();
}