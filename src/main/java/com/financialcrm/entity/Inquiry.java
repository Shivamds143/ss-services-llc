package com.financialcrm.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inquiries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inquiry {

    public enum Status {
        NEW, FOLLOW_UP, DOCUMENTS_PENDING, APPROVED, REJECTED, CLOSED
    }
    public enum Priority {
        LOW, MEDIUM, HIGH, URGENT
    }
    public enum OccupationType {
        SALARIED, SELF_EMPLOYED, PROFESSIONAL, RETIRED, HOMEMAKER, STUDENT, OTHERS
    }
    public enum ResidenceStatus {
        SELF_OWNED, OWNED_BY_FAMILY, RENTED, EMPLOYER_LEASED
    }
    public enum Gender {
        MALE, FEMALE, OTHER
    }
    public enum MaritalStatus {
        SINGLE, MARRIED, OTHER
    }
    public enum ResidencyStatus {
        RESIDENT_INDIAN, NON_RESIDENT_INDIAN
    }
    public enum Category {
        SC_ST, OBC, OTHERS
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Core CRM fields (existing) ──────────────────────────────────
    @NotNull(message = "Customer is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @NotNull(message = "Inquiry type is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inquiry_type_id", nullable = false)
    private InquiryType inquiryType;

    @NotNull(message = "Inquiry date is required")
    @Column(name = "inquiry_date", nullable = false)
    private LocalDate inquiryDate;

    @DecimalMin(value = "0.0", message = "Loan amount must be positive")
    @Column(name = "loan_amount", precision = 15, scale = 2)
    private BigDecimal loanAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private Status status = Status.NEW;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority")
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    @Column(name = "bank_name", length = 150)
    private String bankName;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "100.0")
    @Column(name = "interest_rate", precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Min(value = 1)
    @Column(name = "tenure_months")
    private Integer tenureMonths;

    @Column(name = "emi_amount", precision = 15, scale = 2)
    private BigDecimal emiAmount;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "expected_close_date")
    private LocalDate expectedCloseDate;

    @Column(name = "actual_close_date")
    private LocalDate actualCloseDate;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    // ── Insurance-specific fields ────────────────────────────────────────
    /** Broad insurance category: Life Insurance, Health Insurance, General Insurance */
    @Column(name = "insurance_type", length = 50)
    private String insuranceType;

    /** Narrower sub-type: Motor, Term, Travel, Fire, Health, etc. */
    @Column(name = "insurance_sub_type", length = 150)
    private String insuranceSubType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "inquiry", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Followup> followups = new ArrayList<>();

    // ── LIC HFL: Applicant Personal Info ───────────────────────────
    @Column(name = "applicant_pan", length = 10)
    private String applicantPan;

    @Column(name = "applicant_name_as_pan", length = 150)
    private String applicantNameAsPan;

    @Column(name = "applicant_surname", length = 100)
    private String applicantSurname;

    @Column(name = "applicant_first_name", length = 100)
    private String applicantFirstName;

    @Column(name = "applicant_middle_name", length = 100)
    private String applicantMiddleName;

    @Column(name = "applicant_father_husband_name", length = 150)
    private String applicantFatherHusbandName;

    @Column(name = "applicant_dob")
    private LocalDate applicantDob;

    @Enumerated(EnumType.STRING)
    @Column(name = "applicant_gender", length = 10)
    private Gender applicantGender;

    @Enumerated(EnumType.STRING)
    @Column(name = "applicant_marital_status", length = 10)
    private MaritalStatus applicantMaritalStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "applicant_residency_status", length = 25)
    private ResidencyStatus applicantResidencyStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "applicant_category", length = 10)
    private Category applicantCategory;

    @Column(name = "applicant_education", length = 100)
    private String applicantEducation;

    @Column(name = "applicant_institute", length = 150)
    private String applicantInstitute;

    @Column(name = "applicant_dependents_children")
    private Integer applicantDependentsChildren;

    @Column(name = "applicant_dependents_adults")
    private Integer applicantDependentsAdults;

    // ── LIC HFL: Applicant Address ──────────────────────────────────
    @Column(name = "applicant_flat_no", length = 50)
    private String applicantFlatNo;

    @Column(name = "applicant_building", length = 150)
    private String applicantBuilding;

    @Column(name = "applicant_road", length = 150)
    private String applicantRoad;

    @Column(name = "applicant_landmark", length = 150)
    private String applicantLandmark;

    @Column(name = "applicant_area", length = 100)
    private String applicantArea;

    @Column(name = "applicant_city", length = 100)
    private String applicantCity;

    @Column(name = "applicant_state", length = 100)
    private String applicantState;

    @Column(name = "applicant_pin", length = 10)
    private String applicantPin;

    @Column(name = "applicant_phone", length = 20)
    private String applicantPhone;

    @Column(name = "applicant_years_at_residence")
    private Integer applicantYearsAtResidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "applicant_residence_status", length = 20)
    private ResidenceStatus applicantResidenceStatus;

    // ── LIC HFL: Applicant Income Info ─────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "applicant_occupation", length = 20)
    private OccupationType applicantOccupation;

    @Column(name = "applicant_employer_name", length = 200)
    private String applicantEmployerName;

    @Column(name = "applicant_designation", length = 100)
    private String applicantDesignation;

    @Column(name = "applicant_employee_no", length = 50)
    private String applicantEmployeeNo;

    @Column(name = "applicant_department", length = 100)
    private String applicantDepartment;

    @Column(name = "applicant_date_of_joining")
    private LocalDate applicantDateOfJoining;

    @Column(name = "applicant_total_experience")
    private Integer applicantTotalExperience;

    @Column(name = "applicant_retirement_age")
    private Integer applicantRetirementAge;

    @Column(name = "applicant_gross_annual_income", precision = 15, scale = 2)
    private BigDecimal applicantGrossAnnualIncome;

    @Column(name = "applicant_net_annual_income", precision = 15, scale = 2)
    private BigDecimal applicantNetAnnualIncome;

    @Column(name = "applicant_nature_of_business", length = 200)
    private String applicantNatureOfBusiness;

    @Column(name = "applicant_years_in_business")
    private Integer applicantYearsInBusiness;

    // ── LIC HFL: Co-Applicant Personal Info ────────────────────────
    @Column(name = "coapplicant_pan", length = 10)
    private String coapplicantPan;

    @Column(name = "coapplicant_name_as_pan", length = 150)
    private String coapplicantNameAsPan;

    @Column(name = "coapplicant_surname", length = 100)
    private String coapplicantSurname;

    @Column(name = "coapplicant_first_name", length = 100)
    private String coapplicantFirstName;

    @Column(name = "coapplicant_middle_name", length = 100)
    private String coapplicantMiddleName;

    @Column(name = "coapplicant_father_husband_name", length = 150)
    private String coapplicantFatherHusbandName;

    @Column(name = "coapplicant_dob")
    private LocalDate coapplicantDob;

    @Enumerated(EnumType.STRING)
    @Column(name = "coapplicant_gender", length = 10)
    private Gender coapplicantGender;

    @Enumerated(EnumType.STRING)
    @Column(name = "coapplicant_marital_status", length = 10)
    private MaritalStatus coapplicantMaritalStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "coapplicant_residency_status", length = 25)
    private ResidencyStatus coapplicantResidencyStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "coapplicant_category", length = 10)
    private Category coapplicantCategory;

    @Column(name = "coapplicant_education", length = 100)
    private String coapplicantEducation;

    @Column(name = "coapplicant_occupation", length = 20)
    @Enumerated(EnumType.STRING)
    private OccupationType coapplicantOccupation;

    @Column(name = "coapplicant_employer_name", length = 200)
    private String coapplicantEmployerName;

    @Column(name = "coapplicant_gross_annual_income", precision = 15, scale = 2)
    private BigDecimal coapplicantGrossAnnualIncome;

    @Column(name = "coapplicant_net_annual_income", precision = 15, scale = 2)
    private BigDecimal coapplicantNetAnnualIncome;

    @Column(name = "coapplicant_relation", length = 100)
    private String coapplicantRelation;

    // ── LIC HFL: Property Info ──────────────────────────────────────
    @Column(name = "property_flat_no", length = 50)
    private String propertyFlatNo;

    @Column(name = "property_floor_no", length = 20)
    private String propertyFloorNo;

    @Column(name = "property_building", length = 150)
    private String propertyBuilding;

    @Column(name = "property_area", length = 100)
    private String propertyArea;

    @Column(name = "property_landmark", length = 150)
    private String propertyLandmark;

    @Column(name = "property_city", length = 100)
    private String propertyCity;

    @Column(name = "property_state", length = 100)
    private String propertyState;

    @Column(name = "property_pin", length = 10)
    private String propertyPin;

    @Column(name = "property_owner_name", length = 200)
    private String propertyOwnerName;

    @Column(name = "property_value", precision = 15, scale = 2)
    private BigDecimal propertyValue;

    // ── LIC HFL: Bank Account ───────────────────────────────────────
    @Column(name = "bank_account_no", length = 30)
    private String bankAccountNo;

    @Column(name = "bank_ifsc", length = 15)
    private String bankIfsc;

    @Column(name = "bank_account_type", length = 20)
    private String bankAccountType;

    @Column(name = "bank_account_holder", length = 150)
    private String bankAccountHolder;

    // ── LIC HFL: Reference 1 ───────────────────────────────────────
    @Column(name = "ref1_name", length = 150)
    private String ref1Name;

    @Column(name = "ref1_address", columnDefinition = "TEXT")
    private String ref1Address;

    @Column(name = "ref1_mobile", length = 15)
    private String ref1Mobile;

    @Column(name = "ref1_email", length = 100)
    private String ref1Email;

    @Column(name = "ref1_relation", length = 50)
    private String ref1Relation;

    // ── LIC HFL: Reference 2 ───────────────────────────────────────
    @Column(name = "ref2_name", length = 150)
    private String ref2Name;

    @Column(name = "ref2_address", columnDefinition = "TEXT")
    private String ref2Address;

    @Column(name = "ref2_mobile", length = 15)
    private String ref2Mobile;

    @Column(name = "ref2_email", length = 100)
    private String ref2Email;

    @Column(name = "ref2_relation", length = 50)
    private String ref2Relation;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (inquiryDate == null) inquiryDate = LocalDate.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}