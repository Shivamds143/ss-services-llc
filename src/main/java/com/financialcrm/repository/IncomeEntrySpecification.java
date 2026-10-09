package com.financialcrm.repository;

import com.financialcrm.entity.IncomeEntry;
import com.financialcrm.entity.IncomeEntry.PaymentMode;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specification builder for dynamic Income Entry filtering.
 * Used by IncomeEntryRepository (JpaSpecificationExecutor).
 *
 * Usage:
 *   Specification<IncomeEntry> spec = IncomeEntrySpecification.build(filter);
 *   repository.findAll(spec, pageable);
 */
public class IncomeEntrySpecification {

    private IncomeEntrySpecification() { /* utility class */ }

    /**
     * Builds a composite Specification from the given filter parameters.
     * Any null parameter is silently ignored (no restriction applied for it).
     */
    public static Specification<IncomeEntry> build(
            LocalDate   startDate,
            LocalDate   endDate,
            Long        categoryId,
            String      categoryGroup,
            Long        employeeId,
            PaymentMode paymentMode,
            java.math.BigDecimal minAmount,
            java.math.BigDecimal maxAmount,
            String      search          // customer name OR transaction reference
    ) {
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // Eagerly fetch associations to avoid N+1 on paged result
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("incomeCategory", JoinType.LEFT);
                root.fetch("customer",       JoinType.LEFT);
                root.fetch("employee",       JoinType.LEFT);
                root.fetch("createdBy",      JoinType.LEFT);
            }

            // ── Date range ─────────────────────────────────────────────────
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("incomeDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("incomeDate"), endDate));
            }

            // ── Category ───────────────────────────────────────────────────
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("incomeCategory").get("id"), categoryId));
            }

            // ── Category Group ─────────────────────────────────────────────
            if (categoryGroup != null && !categoryGroup.isBlank()) {
                predicates.add(cb.equal(root.get("incomeCategory").get("categoryGroup"), categoryGroup));
            }

            // ── Employee ───────────────────────────────────────────────────
            if (employeeId != null) {
                predicates.add(cb.equal(root.get("employee").get("id"), employeeId));
            }

            // ── Payment Mode ───────────────────────────────────────────────
            if (paymentMode != null) {
                predicates.add(cb.equal(root.get("paymentMode"), paymentMode));
            }

            // ── Amount range ───────────────────────────────────────────────
            if (minAmount != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), minAmount));
            }
            if (maxAmount != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("amount"), maxAmount));
            }

            // ── Free-text search (customer name OR transaction reference) ──
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Join<Object, Object> customerJoin = root.join("customer", JoinType.LEFT);
                Predicate byCustomer   = cb.like(cb.lower(customerJoin.get("customerName")), pattern);
                Predicate byReference  = cb.like(cb.lower(root.get("transactionReference")), pattern);
                Predicate byRemarks    = cb.like(cb.lower(root.get("remarks")), pattern);
                predicates.add(cb.or(byCustomer, byReference, byRemarks));
            }

            query.orderBy(
                cb.desc(root.get("incomeDate")),
                cb.desc(root.get("id"))
            );

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}