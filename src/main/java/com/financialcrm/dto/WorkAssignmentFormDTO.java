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
public class WorkAssignmentFormDTO {

    @NotNull(message = "Employee is required")
    private Long employeeId;

    @NotNull(message = "Work date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate workDate;

    @Min(value = 0, message = "Daily leads cannot be negative")
    private Integer dailyLeads;
    private String leadsNotes;
    @Min(value = 0, message = "Total calls cannot be negative")
    private Integer totalCalls;
    @Min(value = 0, message = "Connected calls cannot be negative")
    private Integer connectedCalls;
    @Min(value = 0, message = "Not answered calls cannot be negative")
    private Integer notAnswered;
    @Min(value = 0, message = "Busy calls cannot be negative")
    private Integer busyCalls;
    private String callNotes;
    private String workSummary;

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

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDate;

    private String workProgress;
    @Min(value = 0, message = "Completion percentage cannot be negative")
    @Max(value = 100, message = "Completion percentage cannot exceed 100")
    private Integer completionPercentage;
    private String adminNotes;
    private String employeeNotes;

    private String returnView;
    private Integer returnMonth;
    private Integer returnYear;
    private Boolean returnIncludeDeleted;
}
