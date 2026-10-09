package com.financialcrm.dto;

import com.financialcrm.entity.EmployeeDailyWork;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class BulkWorkAssignmentFormDTO {

    public enum CopyMode {
        NONE,
        PREVIOUS_DAY,
        PREVIOUS_WEEK
    }

    private Long employeeId;
    private boolean assignAllEmployees;
    @Min(value = 1, message = "Range days must be at least 1")
    private Integer rangeDays;

    @NotNull(message = "Start date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    private CopyMode copyMode = CopyMode.NONE;

    private String workTitle;
    private String workDescription;
    private EmployeeDailyWork.WorkCategory workCategory;
    private EmployeeDailyWork.WorkPriority workPriority;
    private EmployeeDailyWork.WorkStatus workStatus;
    private String workRemarks;
    @Min(value = 0, message = "Expected calls target cannot be negative")
    private Integer expectedCallsTarget;
    @Min(value = 0, message = "Expected leads target cannot be negative")
    private Integer expectedLeadsTarget;
    private String workProgress;
    @Min(value = 0, message = "Completion percentage cannot be negative")
    @Max(value = 100, message = "Completion percentage cannot exceed 100")
    private Integer completionPercentage;
    private String adminNotes;
}
