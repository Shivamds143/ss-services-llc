package com.financialcrm.validation;

import com.financialcrm.dto.IncomeEntryFormDTO;
import com.financialcrm.entity.IncomeEntry.PaymentMode;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Custom Spring Validator for IncomeEntryFormDTO.
 *
 * Handles cross-field business rules that cannot be expressed
 * with simple Jakarta Bean Validation annotations alone.
 *
 * Usage in controller (optional – plain @Valid already covers annotation rules):
 *
 *   @InitBinder("formDTO")
 *   protected void initBinder(WebDataBinder binder) {
 *       binder.addValidators(incomeEntryValidator);
 *   }
 */
@Component
public class IncomeEntryValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return IncomeEntryFormDTO.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        IncomeEntryFormDTO dto = (IncomeEntryFormDTO) target;

        // ── Rule 1: Income date must not be in the future ──────────────────
        if (dto.getIncomeDate() != null && dto.getIncomeDate().isAfter(LocalDate.now())) {
            errors.rejectValue("incomeDate", "income.date.future",
                "Income date cannot be a future date");
        }

        // ── Rule 2: Amount sanity cap (prevent accidental huge entries) ────
        if (dto.getAmount() != null
                && dto.getAmount().compareTo(new BigDecimal("99999999.99")) > 0) {
            errors.rejectValue("amount", "income.amount.tooLarge",
                "Amount exceeds the maximum allowed value of ₹9,99,99,999.99");
        }

        // ── Rule 3: Cheque/NEFT/Bank Transfer must have reference ──────────
        if (dto.getPaymentMode() != null
                && requiresReference(dto.getPaymentMode())
                && (dto.getTransactionReference() == null
                    || dto.getTransactionReference().isBlank())) {
            errors.rejectValue("transactionReference",
                "income.reference.required",
                "Transaction reference / cheque number is required for " +
                dto.getPaymentMode().getDisplayName() + " payments");
        }

        // ── Rule 4: Date must not be older than 5 years ────────────────────
        if (dto.getIncomeDate() != null
                && dto.getIncomeDate().isBefore(LocalDate.now().minusYears(5))) {
            errors.rejectValue("incomeDate", "income.date.tooOld",
                "Income date cannot be older than 5 years");
        }
    }

    private boolean requiresReference(PaymentMode mode) {
        return mode == PaymentMode.CHEQUE
            || mode == PaymentMode.BANK_TRANSFER
            || mode == PaymentMode.CREDIT_CARD;
    }
}
