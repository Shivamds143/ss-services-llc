-- ============================================================
--  Financial CRM & Reception Management System
--  COMPLETE MySQL Database — Fixed & Production-Ready
--  MySQL 8.0+  |  Engine: InnoDB  |  Charset: utf8mb4
--
--  Tables (19 total):
--    01. roles                 08. employees              15. income_categories
--    02. users                 09. attendance             16. income_entries
--    03. user_roles            10. employee_targets       17. income          ← ADDED (was missing)
--    04. customers             11. employee_daily_work    18. notifications
--    05. inquiry_types         12. appointments           19. audit_logs
--    06. inquiries             13. expense_categories
--    07. followups             14. expenses
--
--  Passwords: plain-text (NoOpPasswordEncoder)
--    admin        → Admin@123
--    others       → Pass@1234
--
--  HOW TO RUN:
--    mysql -u root -p < financial_crm_complete.sql
--  OR paste entire file into MySQL Workbench and execute.
-- ============================================================

DROP DATABASE IF EXISTS financial_crm;
CREATE DATABASE financial_crm
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE financial_crm;

SET FOREIGN_KEY_CHECKS = 0;
SET SQL_MODE = 'NO_AUTO_VALUE_ON_ZERO';

-- ============================================================
-- 01. ROLES
-- ============================================================
CREATE TABLE roles (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    role_name   VARCHAR(50)  NOT NULL,
    description VARCHAR(255) NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_roles     PRIMARY KEY (id),
    CONSTRAINT uq_role_name UNIQUE      (role_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 02. USERS
-- ============================================================
CREATE TABLE users (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    username              VARCHAR(100) NOT NULL,
    email                 VARCHAR(150) NOT NULL,
    password              VARCHAR(255) NOT NULL,
    full_name             VARCHAR(150) NOT NULL,
    mobile                VARCHAR(15)  NULL,
    is_active             BOOLEAN      NOT NULL DEFAULT TRUE,
    is_locked             BOOLEAN      NOT NULL DEFAULT FALSE,
    failed_attempts       INT          NOT NULL DEFAULT 0,
    last_login            DATETIME     NULL,
    password_reset_token  VARCHAR(255) NULL,
    password_reset_expiry DATETIME     NULL,
    profile_photo         VARCHAR(500) NULL,
    created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_users      PRIMARY KEY (id),
    CONSTRAINT uq_username   UNIQUE (username),
    CONSTRAINT uq_user_email UNIQUE (email),
    INDEX idx_users_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 03. USER_ROLES (junction)
-- ============================================================
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 04. CUSTOMERS
-- ============================================================
CREATE TABLE customers (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    customer_name    VARCHAR(150) NOT NULL,
    mobile_number    VARCHAR(15)  NOT NULL,
    alternate_mobile VARCHAR(15)  NULL,
    email            VARCHAR(150) NULL,
    inquiry_date     DATE         NOT NULL,
    reference_name   VARCHAR(150) NULL,
    address          TEXT         NULL,
    city             VARCHAR(100) NULL,
    state            VARCHAR(100) NULL,
    pincode          VARCHAR(10)  NULL,
    date_of_birth    DATE         NULL,
    anniversary_date DATE         NULL,
    notes            TEXT         NULL,
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by       BIGINT       NULL,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_customers       PRIMARY KEY (id),
    CONSTRAINT fk_cust_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    INDEX idx_cust_mobile       (mobile_number),
    INDEX idx_cust_name         (customer_name),
    INDEX idx_cust_active       (is_active),
    INDEX idx_cust_inquiry_date (inquiry_date),
    INDEX idx_cust_dob          (date_of_birth),
    INDEX idx_cust_anniversary  (anniversary_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 05. INQUIRY_TYPES (master / lookup)
    next_followup_date     DATE         NULL,
    next_followup_time     TIME         NULL,
    next_followup_reason   VARCHAR(255) NULL,
    call_reminder          BOOLEAN      NOT NULL DEFAULT FALSE,
    call_reminder_time     TIME         NULL,
    call_reminder_sent     BOOLEAN      NOT NULL DEFAULT FALSE,
    whatsapp_reminder      BOOLEAN      NOT NULL DEFAULT FALSE,
    whatsapp_reminder_time TIME         NULL,
    whatsapp_reminder_sent BOOLEAN      NOT NULL DEFAULT FALSE,
    whatsapp_message_template TEXT      NULL,
    email_reminder         BOOLEAN      NOT NULL DEFAULT FALSE,
    reminder_sent          BOOLEAN      NOT NULL DEFAULT FALSE,
    auto_reminder_enabled  BOOLEAN      NOT NULL DEFAULT TRUE,
    reminder_days_before   INT          NOT NULL DEFAULT 0,
    reminder_sent_at       DATETIME     NULL,
    followup_notes         TEXT         NULL,
    internal_notes         TEXT         NULL,
    customer_feedback      TEXT         NULL,
    status                 ENUM('PENDING','COMPLETED','MISSED','RESCHEDULED') NOT NULL DEFAULT 'PENDING',
    outcome                ENUM('POSITIVE','NEUTRAL','NEGATIVE','NO_RESPONSE') NOT NULL DEFAULT 'NEUTRAL',
    assigned_to            BIGINT       NULL,
    created_by             BIGINT       NOT NULL,
    created_at             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_followups     PRIMARY KEY (id),
    CONSTRAINT fk_fu_inquiry    FOREIGN KEY (inquiry_id)  REFERENCES inquiries(id) ON DELETE CASCADE,
    CONSTRAINT fk_fu_customer   FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_fu_assigned   FOREIGN KEY (assigned_to) REFERENCES users(id),
    CONSTRAINT fk_fu_created_by FOREIGN KEY (created_by)  REFERENCES users(id),
    INDEX idx_fu_date      (followup_date),
    INDEX idx_fu_status    (status),
    INDEX idx_fu_inquiry   (inquiry_id),
    INDEX idx_fu_customer  (customer_id),
    INDEX idx_fu_next_date (next_followup_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 08. EMPLOYEES
-- ============================================================
CREATE TABLE employees (
    id                       BIGINT         NOT NULL AUTO_INCREMENT,
    employee_id              VARCHAR(20)    NOT NULL,
    user_id                  BIGINT         NULL,
    employee_name            VARCHAR(150)   NOT NULL,
    father_name              VARCHAR(150)   NULL,
    mother_name              VARCHAR(150)   NULL,
    date_of_birth            DATE           NULL,
    gender                   ENUM('MALE','FEMALE','OTHER') NULL,
    mobile_number            VARCHAR(15)    NOT NULL,
    alternate_mobile         VARCHAR(15)    NULL,
    email                    VARCHAR(150)   NULL,
    address                  TEXT           NULL,
    city                     VARCHAR(100)   NULL,
    state                    VARCHAR(100)   NULL,
    pincode                  VARCHAR(10)    NULL,
    designation              VARCHAR(100)   NULL,
    department               VARCHAR(100)   NULL,
    joining_date             DATE           NOT NULL,
    leaving_date             DATE           NULL,
    salary                   DECIMAL(12, 2) NULL,
    bank_account_number      VARCHAR(50)    NULL,
    bank_name                VARCHAR(100)   NULL,
    ifsc_code                VARCHAR(20)    NULL,
    pan_number               VARCHAR(20)    NULL,
    aadhar_number            VARCHAR(20)    NULL,
    photo_path               VARCHAR(500)   NULL,
    emergency_contact_name   VARCHAR(150)   NULL,
    emergency_contact_mobile VARCHAR(15)    NULL,
    is_active                BOOLEAN        NOT NULL DEFAULT TRUE,
    created_by               BIGINT         NULL,
    created_at               DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_employees      PRIMARY KEY (id),
    CONSTRAINT uq_employee_id    UNIQUE (employee_id),
    CONSTRAINT uq_emp_user_id    UNIQUE (user_id),
    CONSTRAINT fk_emp_user       FOREIGN KEY (user_id)    REFERENCES users(id),
    CONSTRAINT fk_emp_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    INDEX idx_emp_name       (employee_name),
    INDEX idx_emp_mobile     (mobile_number),
    INDEX idx_emp_active     (is_active),
    INDEX idx_emp_department (department)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 09. ATTENDANCE
-- ============================================================
CREATE TABLE attendance (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    employee_id     BIGINT        NOT NULL,
    attendance_date DATE          NOT NULL,
    attendance_type ENUM('PRESENT','ABSENT','HALF_DAY','LEAVE','HOLIDAY','WEEK_OFF') NOT NULL,
    in_time         TIME          NULL,
    out_time        TIME          NULL,
    working_hours   DECIMAL(4, 2) NULL,
    overtime_hours  DECIMAL(4, 2) NOT NULL DEFAULT 0.00,
    remarks         VARCHAR(255)  NULL,
    is_regularized  BOOLEAN       NOT NULL DEFAULT FALSE,
    regularized_by  BIGINT        NULL,
    created_by      BIGINT        NULL,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_attendance         PRIMARY KEY (id),
    CONSTRAINT uq_att_emp_date       UNIQUE (employee_id, attendance_date),
    CONSTRAINT fk_att_employee       FOREIGN KEY (employee_id)    REFERENCES employees(id) ON DELETE CASCADE,
    CONSTRAINT fk_att_regularized_by FOREIGN KEY (regularized_by) REFERENCES users(id),
    CONSTRAINT fk_att_created_by     FOREIGN KEY (created_by)     REFERENCES users(id),
    INDEX idx_att_date     (attendance_date),
    INDEX idx_att_type     (attendance_type),
    INDEX idx_att_employee (employee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 10. EMPLOYEE_TARGETS
-- ============================================================
CREATE TABLE employee_targets (
    id                  BIGINT         NOT NULL AUTO_INCREMENT,
    employee_id         BIGINT         NOT NULL,
    target_month        INT            NOT NULL COMMENT '1=Jan to 12=Dec',
    target_year         INT            NOT NULL,
    leads_target        INT            NOT NULL DEFAULT 0,
    calls_target        INT            NOT NULL DEFAULT 0,
    conversion_target   INT            NOT NULL DEFAULT 0,
    revenue_target      DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    leads_achieved      INT            NOT NULL DEFAULT 0,
    calls_achieved      INT            NOT NULL DEFAULT 0,
    conversion_achieved INT            NOT NULL DEFAULT 0,
    revenue_achieved    DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    remarks             TEXT           NULL,
    set_by              BIGINT         NULL,
    created_at          DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_emp_targets           PRIMARY KEY (id),
    CONSTRAINT uq_emp_target_month_year UNIQUE (employee_id, target_month, target_year),
    CONSTRAINT fk_et_employee           FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    CONSTRAINT fk_et_set_by             FOREIGN KEY (set_by)      REFERENCES users(id),
    INDEX idx_et_employee   (employee_id),
    INDEX idx_et_month_year (target_year, target_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 11. EMPLOYEE_DAILY_WORK
-- ============================================================
CREATE TABLE employee_daily_work (
    id              BIGINT   NOT NULL AUTO_INCREMENT,
    employee_id     BIGINT   NOT NULL,
    work_date       DATE     NOT NULL,
    daily_leads     INT      NOT NULL DEFAULT 0,
    leads_notes     TEXT     NULL,
    total_calls     INT      NOT NULL DEFAULT 0,
    connected_calls INT      NOT NULL DEFAULT 0,
    not_answered    INT      NOT NULL DEFAULT 0,
    busy_calls      INT      NOT NULL DEFAULT 0,
    call_notes      TEXT     NULL,
    work_summary    TEXT     NULL,
    work_title      VARCHAR(200) NULL,
    work_description TEXT    NULL,
    work_category   VARCHAR(50) NOT NULL DEFAULT 'OTHER',
    work_priority   VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    work_status     VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    work_remarks    TEXT     NULL,
    expected_calls_target INT NOT NULL DEFAULT 0,
    expected_leads_target INT NOT NULL DEFAULT 0,
    due_date        DATE     NULL,
    work_progress   VARCHAR(255) NULL,
    completion_percentage INT NOT NULL DEFAULT 0,
    assigned_by     BIGINT   NULL,
    assigned_date_time DATETIME NULL,
    completed_date_time DATETIME NULL,
    admin_notes     TEXT     NULL,
    employee_notes  TEXT     NULL,
    is_deleted      BOOLEAN  NOT NULL DEFAULT FALSE,
    deleted_at      DATETIME NULL,
    deleted_by      BIGINT   NULL,
    restored_at     DATETIME NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_emp_daily_work PRIMARY KEY (id),
    CONSTRAINT uq_emp_work_date  UNIQUE (employee_id, work_date),
    CONSTRAINT fk_edw_employee   FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    CONSTRAINT fk_edw_assigned_by FOREIGN KEY (assigned_by) REFERENCES users(id),
    CONSTRAINT fk_edw_deleted_by FOREIGN KEY (deleted_by) REFERENCES users(id),
    INDEX idx_edw_employee  (employee_id),
    INDEX idx_edw_work_date (work_date),
    INDEX idx_edw_due_date  (due_date),
    INDEX idx_edw_status    (work_status),
    INDEX idx_edw_deleted   (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 12. APPOINTMENTS
-- ============================================================
CREATE TABLE appointments (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    customer_id      BIGINT       NOT NULL,
    inquiry_id       BIGINT       NULL,
    appointment_type ENUM('CUSTOMER_MEETING','AGENT_APPOINTMENT','BANK_VISIT','DOCUMENT_COLLECTION','OTHER') NOT NULL,
    appointment_date DATE         NOT NULL,
    appointment_time TIME         NOT NULL,
    duration_minutes INT          NOT NULL DEFAULT 30,
    location         VARCHAR(255) NULL,
    description      TEXT         NULL,
    status           ENUM('SCHEDULED','CONFIRMED','COMPLETED','CANCELLED','RESCHEDULED') NOT NULL DEFAULT 'SCHEDULED',
    reminder_sent    BOOLEAN      NOT NULL DEFAULT FALSE,
    reminder_time    DATETIME     NULL,
    assigned_to      BIGINT       NULL,
    notes            TEXT         NULL,
    created_by       BIGINT       NOT NULL,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_appointments    PRIMARY KEY (id),
    CONSTRAINT fk_appt_customer   FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_appt_inquiry    FOREIGN KEY (inquiry_id)  REFERENCES inquiries(id),
    CONSTRAINT fk_appt_assigned   FOREIGN KEY (assigned_to) REFERENCES users(id),
    CONSTRAINT fk_appt_created_by FOREIGN KEY (created_by)  REFERENCES users(id),
    INDEX idx_appt_date     (appointment_date),
    INDEX idx_appt_status   (status),
    INDEX idx_appt_customer (customer_id),
    INDEX idx_appt_assigned (assigned_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 13. EXPENSE_CATEGORIES (hierarchical)
-- ============================================================
CREATE TABLE expense_categories (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    category_name VARCHAR(100) NOT NULL,
    category_code VARCHAR(50)  NOT NULL,
    description   VARCHAR(255) NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order    INT          NOT NULL DEFAULT 0,
    parent_id     BIGINT       NULL COMMENT 'NULL = top-level; non-null = sub-category',
    CONSTRAINT pk_expense_cat    PRIMARY KEY (id),
    CONSTRAINT uq_exp_cat_name   UNIQUE (category_name),
    CONSTRAINT uq_exp_cat_code   UNIQUE (category_code),
    CONSTRAINT fk_exp_cat_parent FOREIGN KEY (parent_id) REFERENCES expense_categories(id) ON DELETE SET NULL,
    INDEX idx_exp_cat_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 14. EXPENSES
-- ============================================================
CREATE TABLE expenses (
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    expense_date    DATE           NOT NULL,
    category_id     BIGINT         NOT NULL,
    sub_category_id BIGINT         NULL,
    amount          DECIMAL(12, 2) NOT NULL,
    payment_mode    ENUM('CASH','CHEQUE','ONLINE_TRANSFER','UPI','CARD') NOT NULL,
    vendor_name     VARCHAR(150)   NULL,
    bill_number     VARCHAR(100)   NULL,
    description     TEXT           NULL,
    notes           TEXT           NULL,
    bill_photo      VARCHAR(500)   NULL,
    is_approved     BOOLEAN        NOT NULL DEFAULT FALSE,
    approved_by     BIGINT         NULL,
    created_by      BIGINT         NOT NULL,
    created_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_expenses         PRIMARY KEY (id),
    CONSTRAINT fk_exp_category     FOREIGN KEY (category_id)     REFERENCES expense_categories(id),
    CONSTRAINT fk_exp_sub_category FOREIGN KEY (sub_category_id) REFERENCES expense_categories(id),
    CONSTRAINT fk_exp_approved_by  FOREIGN KEY (approved_by)     REFERENCES users(id),
    CONSTRAINT fk_exp_created_by   FOREIGN KEY (created_by)      REFERENCES users(id),
    CONSTRAINT chk_exp_amount      CHECK (amount > 0),
    INDEX idx_exp_date         (expense_date),
    INDEX idx_exp_category     (category_id),
    INDEX idx_exp_payment_mode (payment_mode),
    INDEX idx_exp_approved     (is_approved)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 15. INCOME_CATEGORIES (master — 63 categories, 10 groups)
-- ============================================================
CREATE TABLE income_categories (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    category_name  VARCHAR(150) NOT NULL,
    category_group VARCHAR(100) NOT NULL,
    description    VARCHAR(500) NULL,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_income_cat      PRIMARY KEY (id),
    CONSTRAINT uq_income_cat_name UNIQUE (category_name),
    INDEX idx_inc_cat_group  (category_group),
    INDEX idx_inc_cat_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 16. INCOME_ENTRIES (transaction table — used by IncomeEntry.java)
-- ============================================================
CREATE TABLE income_entries (
    id                    BIGINT         NOT NULL AUTO_INCREMENT,
    income_date           DATE           NOT NULL,
    customer_id           BIGINT         NULL,
    income_category_id    BIGINT         NOT NULL,
    amount                DECIMAL(12, 2) NOT NULL,
    payment_mode          VARCHAR(30)    NOT NULL COMMENT 'CASH | UPI | BANK_TRANSFER | CHEQUE | CREDIT_CARD',
    transaction_reference VARCHAR(100)   NULL,
    remarks               TEXT           NULL,
    employee_id           BIGINT         NULL,
    created_by            BIGINT         NOT NULL,
    created_at            DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_income_entries     PRIMARY KEY (id),
    CONSTRAINT fk_ie_customer        FOREIGN KEY (customer_id)        REFERENCES customers(id)        ON DELETE SET NULL,
    CONSTRAINT fk_ie_income_category FOREIGN KEY (income_category_id) REFERENCES income_categories(id),
    CONSTRAINT fk_ie_employee        FOREIGN KEY (employee_id)        REFERENCES employees(id)        ON DELETE SET NULL,
    CONSTRAINT fk_ie_created_by      FOREIGN KEY (created_by)         REFERENCES users(id),
    CONSTRAINT chk_ie_payment_mode   CHECK (payment_mode IN ('CASH','UPI','BANK_TRANSFER','CHEQUE','CREDIT_CARD')),
    CONSTRAINT chk_ie_amount         CHECK (amount > 0),
    INDEX idx_ie_income_date   (income_date),
    INDEX idx_ie_customer      (customer_id),
    INDEX idx_ie_category      (income_category_id),
    INDEX idx_ie_employee      (employee_id),
    INDEX idx_ie_payment_mode  (payment_mode),
    INDEX idx_ie_created_by    (created_by),
    INDEX idx_ie_date_category (income_date, income_category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 17. INCOME  ← ADDED (used by Income.java entity, @Table(name="income"))
--     Used by DashboardController for monthly income summaries
-- ============================================================
CREATE TABLE income (
    id               BIGINT         NOT NULL AUTO_INCREMENT,
    income_date      DATE           NOT NULL,
    category_id      BIGINT         NOT NULL,
    inquiry_id       BIGINT         NULL,
    customer_id      BIGINT         NULL,
    amount           DECIMAL(12, 2) NOT NULL,
    payment_mode     ENUM('CASH','CHEQUE','ONLINE_TRANSFER','UPI','CARD') NOT NULL,
    reference_number VARCHAR(100)   NULL,
    description      TEXT           NULL,
    notes            TEXT           NULL,
    created_by       BIGINT         NOT NULL,
    created_at       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_income          PRIMARY KEY (id),
    CONSTRAINT fk_inc_category    FOREIGN KEY (category_id)  REFERENCES income_categories(id),
    CONSTRAINT fk_inc_inquiry     FOREIGN KEY (inquiry_id)   REFERENCES inquiries(id)   ON DELETE SET NULL,
    CONSTRAINT fk_inc_customer    FOREIGN KEY (customer_id)  REFERENCES customers(id)   ON DELETE SET NULL,
    CONSTRAINT fk_inc_created_by  FOREIGN KEY (created_by)   REFERENCES users(id),
    CONSTRAINT chk_inc_amount     CHECK (amount > 0),
    INDEX idx_inc_date        (income_date),
    INDEX idx_inc_category    (category_id),
    INDEX idx_inc_customer    (customer_id),
    INDEX idx_inc_inquiry     (inquiry_id),
    INDEX idx_inc_payment_mode(payment_mode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 18. NOTIFICATIONS
-- ============================================================
CREATE TABLE notifications (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    user_id           BIGINT       NOT NULL,
    title             VARCHAR(255) NOT NULL,
    message           TEXT         NOT NULL,
    notification_type ENUM('FOLLOWUP_REMINDER','BIRTHDAY_REMINDER','EMI_DUE',
                           'INSURANCE_RENEWAL','APPOINTMENT_REMINDER','SYSTEM','GENERAL') NOT NULL,
    reference_id      BIGINT       NULL,
    reference_type    VARCHAR(50)  NULL,
    is_read           BOOLEAN      NOT NULL DEFAULT FALSE,
    read_at           DATETIME     NULL,
    is_sent_email     BOOLEAN      NOT NULL DEFAULT FALSE,
    is_sent_sms       BOOLEAN      NOT NULL DEFAULT FALSE,
    is_sent_whatsapp  BOOLEAN      NOT NULL DEFAULT FALSE,
    scheduled_at      DATETIME     NULL,
    sent_at           DATETIME     NULL,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_notifications PRIMARY KEY (id),
    CONSTRAINT fk_notif_user    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_notif_user    (user_id),
    INDEX idx_notif_type    (notification_type),
    INDEX idx_notif_read    (is_read),
    INDEX idx_notif_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 19. AUDIT_LOGS
-- ============================================================
CREATE TABLE audit_logs (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    user_id       BIGINT       NULL,
    username      VARCHAR(100) NULL,
    action        VARCHAR(100) NOT NULL,
    entity_type   VARCHAR(100) NULL,
    entity_id     BIGINT       NULL,
    old_value     TEXT         NULL,
    new_value     TEXT         NULL,
    ip_address    VARCHAR(50)  NULL,
    user_agent    VARCHAR(500) NULL,
    status        ENUM('SUCCESS','FAILURE') NOT NULL DEFAULT 'SUCCESS',
    error_message TEXT         NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_audit_logs PRIMARY KEY (id),
    INDEX idx_al_user    (user_id),
    INDEX idx_al_action  (action),
    INDEX idx_al_entity  (entity_type, entity_id),
    INDEX idx_al_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;


CREATE TABLE utility_bills (
    id                   BIGINT NOT NULL AUTO_INCREMENT,
    bill_name            VARCHAR(150) NOT NULL,
    category_id          BIGINT NOT NULL,
    amount               DECIMAL(12,2) NOT NULL,
    due_date             DATE NOT NULL,
    recurrence_type      VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    reminder_days_before INT DEFAULT 3,
    status               VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    expense_id           BIGINT NULL,
    notes                TEXT NULL,
    is_active            BIT(1) NOT NULL DEFAULT 1,
    created_at           DATETIME NOT NULL,
    updated_at           DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_ub_category FOREIGN KEY (category_id) REFERENCES expense_categories(id),
    CONSTRAINT fk_ub_expense  FOREIGN KEY (expense_id)  REFERENCES expenses(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- MASTER DATA & SEED DATA
-- ============================================================

-- ── ROLES ──────────────────────────────────────────────────
INSERT INTO roles (role_name, description) VALUES
('ROLE_ADMIN',        'System administrator — full access to all modules'),
('ROLE_RECEPTIONIST', 'Front-desk staff — customer, inquiry, and appointment management'),
('ROLE_EMPLOYEE',     'Field/sales employee — own inquiries, follow-ups, and daily work');


-- ── USERS (plain-text passwords — NoOpPasswordEncoder) ────
-- admin → Admin@123   |   others → Pass@1234
INSERT INTO users (username, email, password, full_name, mobile, is_active) VALUES
('admin',         'admin@financialcrm.com',        'Admin@123', 'System Administrator', '9999999999', TRUE),
('receptionist1', 'reception@financialcrm.com',    'Pass@1234', 'Pooja Desai',          '9988776655', TRUE),
('rahul.sharma',  'rahul.sharma@financialcrm.com', 'Pass@1234', 'Rahul Sharma',         '9876543210', TRUE),
('priya.patil',   'priya.patil@financialcrm.com',  'Pass@1234', 'Priya Patil',          '9823456780', TRUE),
('amit.joshi',    'amit.joshi@financialcrm.com',   'Pass@1234', 'Amit Joshi',           '9812345678', TRUE);

-- ── ASSIGN ROLES ───────────────────────────────────────────
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON r.role_name = 'ROLE_ADMIN'
WHERE u.username = 'admin';

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON r.role_name = 'ROLE_RECEPTIONIST'
WHERE u.username = 'receptionist1';

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON r.role_name = 'ROLE_EMPLOYEE'
WHERE u.username IN ('rahul.sharma', 'priya.patil', 'amit.joshi');


-- ── INQUIRY TYPES ──────────────────────────────────────────
INSERT INTO inquiry_types (type_name, type_code, description, sort_order) VALUES
('Home Loan',            'HOME_LOAN',       'Residential property purchase or construction',   1),
('Loan Against Property','LAP_LOAN',        'Loan using owned property as collateral',          2),
('Personal Loan',        'PERSONAL_LOAN',   'Unsecured personal loan',                         3),
('Business Loan',        'BUSINESS_LOAN',   'Business expansion or working capital loan',       4),
('Car / Vehicle Loan',   'CAR_LOAN',        'Four-wheeler or two-wheeler financing',            5),
('Education Loan',       'EDUCATION_LOAN',  'Higher education financing',                       6),
('Gold Loan',            'GOLD_LOAN',       'Loan against gold jewellery',                      7),
('Insurance',            'INSURANCE',       'Life, health, vehicle, or general insurance',      8),
('Mutual Fund / SIP',    'MUTUAL_FUND',     'Mutual fund investment and SIP setup',             9),
('Credit Card',          'CREDIT_CARD',     'Credit card application and limit enhancement',   10),
('CIBIL Check',          'CIBIL_CHECK',     'Credit score check and improvement consultation', 11),
('Visit For Sir',        'VISIT_FOR_SIR',   'Direct client visit request',                     12);


-- ── EMPLOYEES ──────────────────────────────────────────────
INSERT INTO employees (employee_id, user_id, employee_name, father_name, date_of_birth,
    gender, mobile_number, email, address, city, state, pincode,
    designation, department, joining_date, salary,
    bank_account_number, bank_name, ifsc_code, pan_number,
    emergency_contact_name, emergency_contact_mobile, is_active, created_by)
VALUES
(
    'EMP001',
    (SELECT id FROM users WHERE username = 'rahul.sharma'),
    'Rahul Sharma', 'Suresh Sharma', '1993-07-22',
    'MALE', '9876543210', 'rahul.sharma@financialcrm.com',
    '14-B, Samarth Nagar', 'Nashik', 'Maharashtra', '422005',
    'Senior Loan Executive', 'Sales', '2022-06-01', 28000.00,
    '50200012345678', 'HDFC Bank', 'HDFC0001234', 'ABCRS1234D',
    'Suresh Sharma', '9876000001', TRUE,
    (SELECT id FROM users WHERE username = 'admin')
),
(
    'EMP002',
    (SELECT id FROM users WHERE username = 'receptionist1'),
    'Pooja Desai', 'Ramesh Desai', '1996-03-18',
    'FEMALE', '9988776655', 'reception@financialcrm.com',
    '7, Shivaji Chowk', 'Nashik', 'Maharashtra', '422001',
    'Receptionist', 'Operations', '2022-09-15', 22000.00,
    '60100098765432', 'SBI', 'SBIN0004567', 'BCDPD5678E',
    'Ramesh Desai', '9988000002', TRUE,
    (SELECT id FROM users WHERE username = 'admin')
),
(
    'EMP003',
    (SELECT id FROM users WHERE username = 'priya.patil'),
    'Priya Patil', 'Dilip Patil', '1995-11-05',
    'FEMALE', '9823456780', 'priya.patil@financialcrm.com',
    '22, Gangapur Road', 'Nashik', 'Maharashtra', '422013',
    'Loan Executive', 'Sales', '2023-01-10', 24000.00,
    '40200056781234', 'ICICI Bank', 'ICIC0002345', 'CDEPP4321F',
    'Dilip Patil', '9823000003', TRUE,
    (SELECT id FROM users WHERE username = 'admin')
),
(
    'EMP004',
    (SELECT id FROM users WHERE username = 'amit.joshi'),
    'Amit Joshi', 'Prakash Joshi', '1991-04-28',
    'MALE', '9812345678', 'amit.joshi@financialcrm.com',
    '56, College Road', 'Nashik', 'Maharashtra', '422005',
    'Insurance Advisor', 'Insurance', '2023-03-20', 26000.00,
    '70100034562345', 'Axis Bank', 'UTIB0001567', 'DEFAJ7890G',
    'Prakash Joshi', '9812000004', TRUE,
    (SELECT id FROM users WHERE username = 'admin')
);


-- ── CUSTOMERS (12 records) ─────────────────────────────────
INSERT INTO customers (customer_name, mobile_number, alternate_mobile, email,
    inquiry_date, reference_name, address, city, state, pincode,
    date_of_birth, anniversary_date, notes, is_active, created_by)
VALUES
('Amit Verma',     '9823001001', '9823001002', 'amit.verma@example.com',
    CURDATE()-INTERVAL 45 DAY, 'Walk-in',    '12, MG Road, Panchvati',    'Nashik','Maharashtra','422003',
    '1985-04-15','2010-11-20','Good CIBIL, salaried, interested in Home Loan.',TRUE,(SELECT id FROM users WHERE username='admin')),
('Priya Singh',    '9823002001', NULL,          'priya.singh@example.com',
    CURDATE()-INTERVAL 30 DAY, 'Amit Verma','5, Park Street, Indira Nagar','Pune','Maharashtra','411001',
    '1990-06-10',NULL,'Self-employed. Needs personal loan for business.',TRUE,(SELECT id FROM users WHERE username='admin')),
('Suresh Patel',   '9823003001', '9823003002', 'suresh.patel@example.com',
    CURDATE()-INTERVAL 20 DAY, 'Online Ad',  '99, Gandhi Nagar',           'Nashik','Maharashtra','422001',
    '1978-12-25','2005-02-14','Wants business loan. ITR pending.',TRUE,(SELECT id FROM users WHERE username='admin')),
('Anita Kulkarni', '9823004001', NULL,          'anita.kulkarni@example.com',
    CURDATE()-INTERVAL 15 DAY, 'Staff referral','33, Tilak Wadi',          'Nashik','Maharashtra','422002',
    '1982-08-22','2008-03-10','Health insurance inquiry.',TRUE,(SELECT id FROM users WHERE username='receptionist1')),
('Rajesh Gupta',   '9823005001', '9823005002', 'rajesh.gupta@example.com',
    CURDATE()-INTERVAL 10 DAY, 'Walk-in',    '8, New Pandit Colony',       'Nashik','Maharashtra','422004',
    '1975-01-17',NULL,'High CIBIL score 790. Interested in LAP.',TRUE,(SELECT id FROM users WHERE username='rahul.sharma')),
('Sunita Mahajan', '9823006001', NULL,          'sunita.mahajan@example.com',
    CURDATE()-INTERVAL 8 DAY,  'Facebook Ad','17, Cidco Colony',           'Nashik','Maharashtra','422009',
    '1993-09-14','2018-02-14','Mutual Fund SIP enquiry.',TRUE,(SELECT id FROM users WHERE username='amit.joshi')),
('Deepak Thakur',  '9823007001', '9823007002', 'deepak.thakur@example.com',
    CURDATE()-INTERVAL 5 DAY,  'Reference - Rajesh Gupta','45, Ambad MIDC Area','Nashik','Maharashtra','422010',
    '1988-03-30',NULL,'Car loan for Maruti Swift.',TRUE,(SELECT id FROM users WHERE username='priya.patil')),
('Kavita Sawant',  '9823008001', NULL,          NULL,
    CURDATE()-INTERVAL 3 DAY,  'Walk-in',    '2, Sharanpur Road',          'Nashik','Maharashtra','422002',
    '1997-07-07',NULL,'Education loan for MBA admission.',TRUE,(SELECT id FROM users WHERE username='receptionist1')),
('Nilesh Borse',   '9823009001', '9823009002', 'nilesh.borse@example.com',
    CURDATE()-INTERVAL 60 DAY, 'Agent referral','78, Dwarka Circle',       'Nashik','Maharashtra','422011',
    '1980-05-19','2007-12-01','Existing customer. Loan renewal.',TRUE,(SELECT id FROM users WHERE username='rahul.sharma')),
('Meena Deshpande','9823010001', NULL,          'meena.d@example.com',
    CURDATE()-INTERVAL 2 DAY,  'Online Form','90, Satpur Colony',          'Nashik','Maharashtra','422007',
    '1986-10-11','2012-06-15','CIBIL improvement needed. Score 640.',TRUE,(SELECT id FROM users WHERE username='admin')),
('Sandeep Pawar',  '9823011001', '9823011002', 'sandeep.pawar@example.com',
    CURDATE()-INTERVAL 1 DAY,  'Walk-in',    '56, Ozar Road',              'Nashik','Maharashtra','422206',
    '1983-02-28','2011-05-20','Home Loan 40L required. Working in HDFC.',TRUE,(SELECT id FROM users WHERE username='rahul.sharma')),
('Rekha Jadhav',   '9823012001', NULL,          NULL,
    CURDATE(),                 'Facebook Ad','12, Mahatma Nagar',          'Nashik','Maharashtra','422007',
    '1995-12-03',NULL,'Term insurance inquiry.',TRUE,(SELECT id FROM users WHERE username='amit.joshi'));


-- ── INQUIRIES (10 records) ─────────────────────────────────
INSERT INTO inquiries (customer_id, inquiry_type_id, inquiry_date, loan_amount,
    status, priority, assigned_to, bank_name, interest_rate, tenure_months,
    emi_amount, remarks, expected_close_date, created_by)
SELECT c.id, it.id, CURDATE()-INTERVAL 45 DAY, 2500000.00,
    'FOLLOW_UP','HIGH',(SELECT id FROM users WHERE username='rahul.sharma'),
    'HDFC Bank',8.75,240,22186.00,
    'Customer has CIBIL 775. Salary slips collected. Processing in progress.',
    CURDATE()+INTERVAL 15 DAY,(SELECT id FROM users WHERE username='rahul.sharma')
FROM customers c, inquiry_types it WHERE c.mobile_number='9823001001' AND it.type_code='HOME_LOAN';

INSERT INTO inquiries (customer_id, inquiry_type_id, inquiry_date, loan_amount,
    status, priority, assigned_to, remarks, expected_close_date, created_by)
SELECT c.id, it.id, CURDATE()-INTERVAL 30 DAY, 500000.00,
    'DOCUMENTS_PENDING','MEDIUM',(SELECT id FROM users WHERE username='priya.patil'),
    'GST returns and bank statements pending from customer.',
    CURDATE()+INTERVAL 20 DAY,(SELECT id FROM users WHERE username='priya.patil')
FROM customers c, inquiry_types it WHERE c.mobile_number='9823002001' AND it.type_code='PERSONAL_LOAN';

INSERT INTO inquiries (customer_id, inquiry_type_id, inquiry_date, loan_amount,
    status, priority, assigned_to, bank_name, interest_rate, tenure_months,
    remarks, expected_close_date, created_by)
SELECT c.id, it.id, CURDATE()-INTERVAL 20 DAY, 1500000.00,
    'NEW','HIGH',(SELECT id FROM users WHERE username='rahul.sharma'),
    'ICICI Bank',10.50,84,
    'ITR for last 3 years and business registration pending.',
    CURDATE()+INTERVAL 30 DAY,(SELECT id FROM users WHERE username='rahul.sharma')
FROM customers c, inquiry_types it WHERE c.mobile_number='9823003001' AND it.type_code='BUSINESS_LOAN';

INSERT INTO inquiries (customer_id, inquiry_type_id, inquiry_date,
    status, priority, assigned_to, remarks, created_by)
SELECT c.id, it.id, CURDATE()-INTERVAL 15 DAY,
    'NEW','LOW',(SELECT id FROM users WHERE username='amit.joshi'),
    'Wants family floater health insurance 5L cover.',
    (SELECT id FROM users WHERE username='amit.joshi')
FROM customers c, inquiry_types it WHERE c.mobile_number='9823004001' AND it.type_code='INSURANCE';

INSERT INTO inquiries (customer_id, inquiry_type_id, inquiry_date, loan_amount,
    status, priority, assigned_to, bank_name, interest_rate, tenure_months,
    remarks, expected_close_date, created_by)
SELECT c.id, it.id, CURDATE()-INTERVAL 10 DAY, 3000000.00,
    'FOLLOW_UP','URGENT',(SELECT id FROM users WHERE username='rahul.sharma'),
    'Kotak Mahindra Bank',9.25,180,
    'Property documents verified. Legal opinion pending.',
    CURDATE()+INTERVAL 10 DAY,(SELECT id FROM users WHERE username='rahul.sharma')
FROM customers c, inquiry_types it WHERE c.mobile_number='9823005001' AND it.type_code='LAP_LOAN';

INSERT INTO inquiries (customer_id, inquiry_type_id, inquiry_date,
    status, priority, assigned_to, remarks, created_by)
SELECT c.id, it.id, CURDATE()-INTERVAL 8 DAY,
    'NEW','LOW',(SELECT id FROM users WHERE username='amit.joshi'),
    'SIP 5000/month. Goal: retirement corpus in 20 years.',
    (SELECT id FROM users WHERE username='amit.joshi')
FROM customers c, inquiry_types it WHERE c.mobile_number='9823006001' AND it.type_code='MUTUAL_FUND';

INSERT INTO inquiries (customer_id, inquiry_type_id, inquiry_date, loan_amount,
    status, priority, assigned_to, bank_name, interest_rate, tenure_months,
    remarks, expected_close_date, created_by)
SELECT c.id, it.id, CURDATE()-INTERVAL 5 DAY, 650000.00,
    'DOCUMENTS_PENDING','MEDIUM',(SELECT id FROM users WHERE username='priya.patil'),
    'SBI',8.90,60,
    'RC copy and insurance pending. Salary account in SBI.',
    CURDATE()+INTERVAL 7 DAY,(SELECT id FROM users WHERE username='priya.patil')
FROM customers c, inquiry_types it WHERE c.mobile_number='9823007001' AND it.type_code='CAR_LOAN';

INSERT INTO inquiries (customer_id, inquiry_type_id, inquiry_date, loan_amount,
    status, priority, assigned_to, remarks, created_by)
SELECT c.id, it.id, CURDATE()-INTERVAL 3 DAY, 1200000.00,
    'NEW','MEDIUM',(SELECT id FROM users WHERE username='rahul.sharma'),
    'MBA from Symbiosis Pune. Fee paid first instalment. Need 12L.',
    (SELECT id FROM users WHERE username='rahul.sharma')
FROM customers c, inquiry_types it WHERE c.mobile_number='9823008001' AND it.type_code='EDUCATION_LOAN';

INSERT INTO inquiries (customer_id, inquiry_type_id, inquiry_date, loan_amount,
    status, priority, assigned_to, bank_name, interest_rate, tenure_months,
    emi_amount, remarks, actual_close_date, created_by)
SELECT c.id, it.id, CURDATE()-INTERVAL 60 DAY, 1800000.00,
    'APPROVED','HIGH',(SELECT id FROM users WHERE username='rahul.sharma'),
    'Axis Bank',9.00,120,18659.00,
    'Loan sanctioned and disbursed. All documents complete.',
    CURDATE()-INTERVAL 10 DAY,(SELECT id FROM users WHERE username='rahul.sharma')
FROM customers c, inquiry_types it WHERE c.mobile_number='9823009001' AND it.type_code='HOME_LOAN';

INSERT INTO inquiries (customer_id, inquiry_type_id, inquiry_date,
    status, priority, assigned_to, remarks, created_by)
SELECT c.id, it.id, CURDATE()-INTERVAL 2 DAY,
    'NEW','MEDIUM',(SELECT id FROM users WHERE username='rahul.sharma'),
    'CIBIL score 640. Multiple credit card defaults. Needs resolution.',
    (SELECT id FROM users WHERE username='admin')
FROM customers c, inquiry_types it WHERE c.mobile_number='9823010001' AND it.type_code='CIBIL_CHECK';


-- ── FOLLOWUPS (4 records) ──────────────────────────────────
INSERT INTO followups (inquiry_id, customer_id, followup_date, followup_time,
    next_followup_date, next_followup_time, next_followup_reason,
    call_reminder, whatsapp_reminder, followup_notes, customer_feedback,
    status, outcome, assigned_to, created_by)
SELECT i.id, c.id, CURDATE()-INTERVAL 10 DAY,'10:30:00',
    CURDATE()+INTERVAL 5 DAY,'11:00:00','Collect updated salary slips and Form 16',
    TRUE, TRUE,
    'Spoke to customer. CIBIL is 775. Agreed to submit docs by this week.',
    'Interested and cooperative.',
    'COMPLETED','POSITIVE',
    (SELECT id FROM users WHERE username='rahul.sharma'),
    (SELECT id FROM users WHERE username='rahul.sharma')
FROM customers c JOIN inquiries i ON i.customer_id=c.id WHERE c.mobile_number='9823001001' LIMIT 1;

INSERT INTO followups (inquiry_id, customer_id, followup_date, followup_time,
    call_reminder, whatsapp_reminder, followup_notes,
    status, outcome, assigned_to, created_by)
SELECT i.id, c.id, CURDATE()+INTERVAL 1 DAY,'14:00:00',
    TRUE, FALSE,
    'Remind about pending GST returns and 6-month bank statement.',
    'PENDING','NEUTRAL',
    (SELECT id FROM users WHERE username='priya.patil'),
    (SELECT id FROM users WHERE username='priya.patil')
FROM customers c JOIN inquiries i ON i.customer_id=c.id WHERE c.mobile_number='9823002001' LIMIT 1;

INSERT INTO followups (inquiry_id, customer_id, followup_date, followup_time,
    call_reminder, whatsapp_reminder, followup_notes,
    status, outcome, assigned_to, created_by)
SELECT i.id, c.id, CURDATE()+INTERVAL 2 DAY,'11:00:00',
    TRUE, TRUE,
    'Property valuation report expected from customer. Check legal opinion status.',
    'PENDING','POSITIVE',
    (SELECT id FROM users WHERE username='rahul.sharma'),
    (SELECT id FROM users WHERE username='rahul.sharma')
FROM customers c JOIN inquiries i ON i.customer_id=c.id WHERE c.mobile_number='9823005001' LIMIT 1;

INSERT INTO followups (inquiry_id, customer_id, followup_date, followup_time,
    followup_notes, status, outcome, assigned_to, created_by)
SELECT i.id, c.id, CURDATE()-INTERVAL 3 DAY,'09:30:00',
    'Customer did not answer. Tried twice.',
    'MISSED','NO_RESPONSE',
    (SELECT id FROM users WHERE username='priya.patil'),
    (SELECT id FROM users WHERE username='priya.patil')
FROM customers c JOIN inquiries i ON i.customer_id=c.id WHERE c.mobile_number='9823007001' LIMIT 1;


-- ── APPOINTMENTS (3 records) ───────────────────────────────
INSERT INTO appointments (customer_id, inquiry_id, appointment_type,
    appointment_date, appointment_time, duration_minutes,
    location, description, status, assigned_to, created_by)
SELECT c.id, i.id, 'DOCUMENT_COLLECTION',
    CURDATE()+INTERVAL 3 DAY,'14:00:00',60,
    'Office','Collect ITR, salary slips, Form 16 and bank passbook.',
    'SCHEDULED',
    (SELECT id FROM users WHERE username='rahul.sharma'),
    (SELECT id FROM users WHERE username='rahul.sharma')
FROM customers c JOIN inquiries i ON i.customer_id=c.id WHERE c.mobile_number='9823001001' LIMIT 1;

INSERT INTO appointments (customer_id, inquiry_id, appointment_type,
    appointment_date, appointment_time, duration_minutes,
    location, description, status, assigned_to, created_by)
SELECT c.id, i.id, 'BANK_VISIT',
    CURDATE()+INTERVAL 5 DAY,'11:00:00',90,
    'Kotak Bank - College Road Branch','File submission for LAP loan. Take all originals.',
    'CONFIRMED',
    (SELECT id FROM users WHERE username='rahul.sharma'),
    (SELECT id FROM users WHERE username='rahul.sharma')
FROM customers c JOIN inquiries i ON i.customer_id=c.id WHERE c.mobile_number='9823005001' LIMIT 1;

INSERT INTO appointments (customer_id, appointment_type,
    appointment_date, appointment_time, duration_minutes,
    location, description, status, assigned_to, created_by)
SELECT c.id, 'CUSTOMER_MEETING',
    CURDATE()+INTERVAL 2 DAY,'16:00:00',45,
    'Office','Present mutual fund plan details and SIP options.',
    'SCHEDULED',
    (SELECT id FROM users WHERE username='amit.joshi'),
    (SELECT id FROM users WHERE username='amit.joshi')
FROM customers c WHERE c.mobile_number='9823006001' LIMIT 1;


-- ── ATTENDANCE (last 7 days for all 4 employees) ───────────
INSERT INTO attendance (employee_id, attendance_date, attendance_type, in_time, out_time, working_hours, created_by)
SELECT e.id, CURDATE()-INTERVAL n DAY,
    CASE WHEN n=6 THEN 'WEEK_OFF' ELSE 'PRESENT' END,
    CASE WHEN n!=6 THEN '09:30:00' ELSE NULL END,
    CASE WHEN n!=6 THEN '18:30:00' ELSE NULL END,
    CASE WHEN n!=6 THEN 9.00 ELSE NULL END,
    (SELECT id FROM users WHERE username='admin')
FROM employees e
JOIN (SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) nums
WHERE e.employee_id IN ('EMP001','EMP002','EMP003','EMP004')
ON DUPLICATE KEY UPDATE working_hours=VALUES(working_hours);


-- ── EMPLOYEE TARGETS (current month) ──────────────────────
INSERT INTO employee_targets (employee_id, target_month, target_year,
    leads_target, calls_target, conversion_target, revenue_target,
    leads_achieved, calls_achieved, conversion_achieved, revenue_achieved,
    remarks, set_by)
SELECT e.id, MONTH(CURDATE()), YEAR(CURDATE()),
    v.lt, v.ct, v.cvt, v.rt, v.la, v.ca, v.cva, v.ra,
    'Monthly targets set by admin',(SELECT id FROM users WHERE username='admin')
FROM employees e
JOIN (
    SELECT 'EMP001' eid,40 lt,250 ct,8 cvt,200000.00 rt,27 la,178 ca,5 cva,132000.00 ra
    UNION SELECT 'EMP002',10,80,2,50000.00,6,55,1,18000.00
    UNION SELECT 'EMP003',30,200,6,150000.00,19,144,4,98000.00
    UNION SELECT 'EMP004',20,100,5,80000.00,14,72,3,48000.00
) v ON e.employee_id=v.eid
ON DUPLICATE KEY UPDATE
    leads_achieved=VALUES(leads_achieved), calls_achieved=VALUES(calls_achieved),
    conversion_achieved=VALUES(conversion_achieved), revenue_achieved=VALUES(revenue_achieved);


-- ── EMPLOYEE DAILY WORK (last 5 working days) ─────────────
INSERT INTO employee_daily_work (employee_id, work_date,
    daily_leads, leads_notes, total_calls, connected_calls, not_answered, busy_calls,
    call_notes, work_summary)
SELECT e.id, CURDATE()-INTERVAL n DAY,
    v.leads, v.lnotes, v.total, v.connected, v.notans, v.busy, v.cnotes, v.summary
FROM employees e
JOIN (SELECT 1 n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) nums
JOIN (
    SELECT 'EMP001' eid,5 leads,'Two home loan leads, one LAP' lnotes,
        28 total,20 connected,6 notans,2 busy,
        'Followed up on HDFC pending cases','Visited 2 customers. Submitted Amit Verma docs to HDFC.' summary
    UNION SELECT 'EMP003',3,'Personal and car loan leads',
        22,16,4,2,'SBI car loan follow-ups','Collected RC copy from Deepak Thakur. Sent to SBI.'
    UNION SELECT 'EMP004',2,'Insurance renewal follow-ups',
        15,11,3,1,'LIC policy renewal reminders','Closed 1 term insurance policy. Sent SIP proposal to Sunita.'
) v ON e.employee_id=v.eid
ON DUPLICATE KEY UPDATE work_summary=VALUES(work_summary);


-- ── EXPENSE CATEGORIES (top-level) ────────────────────────
INSERT INTO expense_categories (category_name, category_code, description, is_active, sort_order) VALUES
('Office Daily Expenses',       'OFFICE_DAILY',       'Day-to-day office running expenses',               TRUE, 1),
('Utility Expenses',            'UTILITY',            'Recurring utility bills and premises costs',        TRUE, 2),
('Employee Expenses',           'EMPLOYEE',           'Salary, allowances, and incentive payouts',         TRUE, 3),
('Marketing Expenses',          'MARKETING',          'Advertising, branding, and promotional material',   TRUE, 4),
('Financial Business Expenses', 'FINANCIAL_BUSINESS', 'Banking charges, processing fees, and legal costs', TRUE, 5),
('Office Maintenance',          'OFFICE_MAINTENANCE', 'Equipment repair, furniture, and infrastructure',   TRUE, 6);

-- Sub-categories: Office Daily
INSERT INTO expense_categories (category_name, category_code, description, is_active, sort_order, parent_id)
SELECT s.n, s.c, s.d, TRUE, s.o, ec.id FROM expense_categories ec
JOIN (
    SELECT 'Tea / Coffee' n,'OFFICE_DAILY_TEA'     c,'Daily tea and coffee'      d,1 o
    UNION SELECT 'Water',      'OFFICE_DAILY_WATER',   'Water cans / RO',            2
    UNION SELECT 'Snacks',     'OFFICE_DAILY_SNACKS',  'Office snacks/refreshments', 3
    UNION SELECT 'Cleaning',   'OFFICE_DAILY_CLEANING','Cleaning and housekeeping',  4
    UNION SELECT 'Stationary', 'OFFICE_DAILY_STAT',    'Pens, pads, registers',      5
) s ON 1=1 WHERE ec.category_code='OFFICE_DAILY';

-- Sub-categories: Utility
INSERT INTO expense_categories (category_name, category_code, description, is_active, sort_order, parent_id)
SELECT s.n, s.c, s.d, TRUE, s.o, ec.id FROM expense_categories ec
JOIN (
    SELECT 'Electricity Bill'      n,'UTIL_ELECTRICITY'  c,'Monthly electricity bill'   d,1 o
    UNION SELECT 'Internet Bill',     'UTIL_INTERNET',       'Broadband / leased line',    2
    UNION SELECT 'Mobile Recharge',   'UTIL_MOBILE',         'Office mobile plans',         3
    UNION SELECT 'Office Rent',       'UTIL_RENT',           'Monthly office rent',         4
    UNION SELECT 'Office EMI',        'UTIL_OFFICE_EMI',     'Office loan EMI',             5
    UNION SELECT 'Building Maintenance','UTIL_BLDG_MAINT',   'Premises upkeep',             6
    UNION SELECT 'Petrol / Fuel',     'UTIL_PETROL',         'Petrol and fuel',             7
    UNION SELECT 'Credit Card Bills', 'UTIL_CC_BILLS',       'Office credit card payments', 8
) s ON 1=1 WHERE ec.category_code='UTILITY';

-- Sub-categories: Employee
INSERT INTO expense_categories (category_name, category_code, description, is_active, sort_order, parent_id)
SELECT s.n, s.c, s.d, TRUE, s.o, ec.id FROM expense_categories ec
JOIN (
    SELECT 'Salary'              n,'EMP_SALARY'   c,'Monthly employee salary' d,1 o
    UNION SELECT 'Incentives',      'EMP_INCENTIVE',  'Performance incentives',   2
    UNION SELECT 'Travel Allowance','EMP_TRAVEL',     'Employee TA / DA',         3
    UNION SELECT 'Petrol Reimbursement','EMP_PETROL', 'Petrol reimbursement',     4
) s ON 1=1 WHERE ec.category_code='EMPLOYEE';

-- Sub-categories: Marketing
INSERT INTO expense_categories (category_name, category_code, description, is_active, sort_order, parent_id)
SELECT s.n, s.c, s.d, TRUE, s.o, ec.id FROM expense_categories ec
JOIN (
    SELECT 'Banners / Hoardings' n,'MKTG_BANNER'   c,'Print banners and flex'  d,1 o
    UNION SELECT 'Facebook Ads',    'MKTG_FACEBOOK',  'Facebook ad campaigns',   2
    UNION SELECT 'Google Ads',      'MKTG_GOOGLE',    'Google PPC campaigns',    3
    UNION SELECT 'Visiting Cards',  'MKTG_CARDS',     'Business card printing',  4
    UNION SELECT 'Pamphlets',       'MKTG_PAMPHLETS', 'Flyers and pamphlets',    5
) s ON 1=1 WHERE ec.category_code='MARKETING';

-- Sub-categories: Financial Business
INSERT INTO expense_categories (category_name, category_code, description, is_active, sort_order, parent_id)
SELECT s.n, s.c, s.d, TRUE, s.o, ec.id FROM expense_categories ec
JOIN (
    SELECT 'CIBIL Pull Charges'     n,'FIN_CIBIL'      c,'CIBIL report pull fee'       d,1 o
    UNION SELECT 'Login Fees',         'FIN_LOGIN',        'File login charges',             2
    UNION SELECT 'Bank Processing Fee','FIN_BANK_PROC',    'Processing fees paid to bank',   3
    UNION SELECT 'Stamp Duty',         'FIN_STAMP',        'Stamp duty on agreements',       4
    UNION SELECT 'Legal Charges',      'FIN_LEGAL',        'Advocate / legal fees',          5
) s ON 1=1 WHERE ec.category_code='FINANCIAL_BUSINESS';

-- Sub-categories: Office Maintenance
INSERT INTO expense_categories (category_name, category_code, description, is_active, sort_order, parent_id)
SELECT s.n, s.c, s.d, TRUE, s.o, ec.id FROM expense_categories ec
JOIN (
    SELECT 'Furniture'      n,'MAINT_FURNITURE' c,'Furniture purchase/repair' d,1 o
    UNION SELECT 'Computer Repair','MAINT_COMPUTER', 'PC / laptop repairs',        2
    UNION SELECT 'Printer Ink',    'MAINT_INK',      'Printer cartridges/toner',   3
    UNION SELECT 'AC Servicing',   'MAINT_AC',       'Air conditioner maintenance', 4
) s ON 1=1 WHERE ec.category_code='OFFICE_MAINTENANCE';


-- ── EXPENSES (10 records) ──────────────────────────────────
INSERT INTO expenses (expense_date, category_id, sub_category_id, amount, payment_mode, vendor_name, bill_number, description, is_approved, approved_by, created_by)
SELECT CURDATE()-INTERVAL 1 DAY,
    (SELECT id FROM expense_categories WHERE category_code='OFFICE_DAILY'),
    (SELECT id FROM expense_categories WHERE category_code='OFFICE_DAILY_TEA'),
    450.00,'CASH','Local Canteen',NULL,'Tea/coffee for staff – June',TRUE,
    (SELECT id FROM users WHERE username='admin'),(SELECT id FROM users WHERE username='admin');

INSERT INTO expenses (expense_date, category_id, sub_category_id, amount, payment_mode, vendor_name, bill_number, description, is_approved, approved_by, created_by)
SELECT CURDATE()-INTERVAL 2 DAY,
    (SELECT id FROM expense_categories WHERE category_code='UTILITY'),
    (SELECT id FROM expense_categories WHERE category_code='UTIL_ELECTRICITY'),
    3850.00,'ONLINE_TRANSFER','MSEDCL','BILL-JUN-2026','Electricity bill June 2026',TRUE,
    (SELECT id FROM users WHERE username='admin'),(SELECT id FROM users WHERE username='admin');

INSERT INTO expenses (expense_date, category_id, sub_category_id, amount, payment_mode, vendor_name, bill_number, description, is_approved, approved_by, created_by)
SELECT CURDATE()-INTERVAL 3 DAY,
    (SELECT id FROM expense_categories WHERE category_code='UTILITY'),
    (SELECT id FROM expense_categories WHERE category_code='UTIL_RENT'),
    15000.00,'CHEQUE','Proprietor - Vijay Builders','CHQ-001','Office rent June 2026',TRUE,
    (SELECT id FROM users WHERE username='admin'),(SELECT id FROM users WHERE username='admin');

INSERT INTO expenses (expense_date, category_id, sub_category_id, amount, payment_mode, vendor_name, bill_number, description, is_approved, approved_by, created_by)
SELECT CURDATE()-INTERVAL 5 DAY,
    (SELECT id FROM expense_categories WHERE category_code='EMPLOYEE'),
    (SELECT id FROM expense_categories WHERE category_code='EMP_SALARY'),
    100000.00,'ONLINE_TRANSFER','Payroll','SAL-2026-06','June 2026 salaries - all staff',TRUE,
    (SELECT id FROM users WHERE username='admin'),(SELECT id FROM users WHERE username='admin');

INSERT INTO expenses (expense_date, category_id, sub_category_id, amount, payment_mode, vendor_name, description, is_approved, approved_by, created_by)
SELECT CURDATE()-INTERVAL 7 DAY,
    (SELECT id FROM expense_categories WHERE category_code='MARKETING'),
    (SELECT id FROM expense_categories WHERE category_code='MKTG_FACEBOOK'),
    5000.00,'CARD','Meta Platforms','Facebook ad campaign - Home Loan June',TRUE,
    (SELECT id FROM users WHERE username='admin'),(SELECT id FROM users WHERE username='admin');

INSERT INTO expenses (expense_date, category_id, sub_category_id, amount, payment_mode, vendor_name, description, is_approved, approved_by, created_by)
SELECT CURDATE()-INTERVAL 8 DAY,
    (SELECT id FROM expense_categories WHERE category_code='FINANCIAL_BUSINESS'),
    (SELECT id FROM expense_categories WHERE category_code='FIN_CIBIL'),
    600.00,'UPI','CIBIL TransUnion','CIBIL report for 6 clients',TRUE,
    (SELECT id FROM users WHERE username='admin'),(SELECT id FROM users WHERE username='admin');

INSERT INTO expenses (expense_date, category_id, sub_category_id, amount, payment_mode, vendor_name, description, is_approved, approved_by, created_by)
SELECT CURDATE()-INTERVAL 10 DAY,
    (SELECT id FROM expense_categories WHERE category_code='OFFICE_MAINTENANCE'),
    (SELECT id FROM expense_categories WHERE category_code='MAINT_INK'),
    1200.00,'CASH','Rathi Computers','HP laserjet toner cartridge',TRUE,
    (SELECT id FROM users WHERE username='admin'),(SELECT id FROM users WHERE username='admin');

INSERT INTO expenses (expense_date, category_id, sub_category_id, amount, payment_mode, vendor_name, description, is_approved, created_by)
SELECT CURDATE()-INTERVAL 12 DAY,
    (SELECT id FROM expense_categories WHERE category_code='EMPLOYEE'),
    (SELECT id FROM expense_categories WHERE category_code='EMP_INCENTIVE'),
    8000.00,'ONLINE_TRANSFER','Payroll','May incentive for Rahul Sharma (2 loan conversions)',FALSE,
    (SELECT id FROM users WHERE username='admin');

INSERT INTO expenses (expense_date, category_id, sub_category_id, amount, payment_mode, vendor_name, bill_number, description, is_approved, approved_by, created_by)
SELECT CURDATE()-INTERVAL 15 DAY,
    (SELECT id FROM expense_categories WHERE category_code='UTILITY'),
    (SELECT id FROM expense_categories WHERE category_code='UTIL_INTERNET'),
    2500.00,'UPI','Airtel Business','INV-AIR-062026','Airtel leased line June 2026',TRUE,
    (SELECT id FROM users WHERE username='admin'),(SELECT id FROM users WHERE username='admin');

INSERT INTO expenses (expense_date, category_id, sub_category_id, amount, payment_mode, vendor_name, description, is_approved, approved_by, created_by)
SELECT CURDATE()-INTERVAL 20 DAY,
    (SELECT id FROM expense_categories WHERE category_code='MARKETING'),
    (SELECT id FROM expense_categories WHERE category_code='MKTG_BANNER'),
    3500.00,'CASH','Nashik Flex Printers','3 flex banners - Home Loan offer June',TRUE,
    (SELECT id FROM users WHERE username='admin'),(SELECT id FROM users WHERE username='admin');


-- ── INCOME CATEGORIES (63 categories, 10 groups) ──────────
INSERT INTO income_categories (category_name, category_group, description) VALUES
-- GROUP 1: Loan Services Income (12)
('Home Loan Processing Fee',          'Loan Services Income',               'Fee for processing home loan applications'),
('Personal Loan Processing Fee',      'Loan Services Income',               'Fee for processing personal loan applications'),
('Business Loan Processing Fee',      'Loan Services Income',               'Fee for processing business loan applications'),
('Car / Vehicle Loan Processing Fee', 'Loan Services Income',               'Fee for processing vehicle loan applications'),
('Loan Against Property Fee',         'Loan Services Income',               'Fee for loans against property collateral'),
('Education Loan Processing Fee',     'Loan Services Income',               'Fee for education loan facilitation'),
('Gold Loan Processing Fee',          'Loan Services Income',               'Fee for gold loan applications'),
('Loan Balance Transfer Fee',         'Loan Services Income',               'Fee for transferring loan to another bank'),
('Top-Up Loan Processing Fee',        'Loan Services Income',               'Fee for top-up over an existing loan'),
('OD / CC Limit Processing Fee',      'Loan Services Income',               'Fee for overdraft or cash-credit limit setup'),
('Loan Renewal / Extension Fee',      'Loan Services Income',               'Fee for renewal or extension of existing loans'),
('DSA / Referral Commission',         'Loan Services Income',               'Commission earned as DSA from bank/NBFC'),
-- GROUP 2: Insurance Income (8)
('Life Insurance Premium Commission', 'Insurance Income',                   'Commission on life insurance policy sales'),
('Health Insurance Commission',       'Insurance Income',                   'Commission on health/mediclaim policy sales'),
('Vehicle Insurance Commission',      'Insurance Income',                   'Commission from motor/vehicle insurance'),
('General Insurance Commission',      'Insurance Income',                   'Commission from fire, property, other general insurance'),
('Term Insurance Commission',         'Insurance Income',                   'Commission from term plan sales'),
('ULIP Commission',                   'Insurance Income',                   'Commission from Unit Linked Insurance Plans'),
('Accidental Insurance Commission',   'Insurance Income',                   'Commission from accidental/disability policies'),
('Insurance Renewal Commission',      'Insurance Income',                   'Commission on renewal of existing policies'),
-- GROUP 3: Mutual Fund & Investment Income (8)
('Mutual Fund Commission (New SIP)',  'Mutual Fund & Investment Income',    'Commission on new SIP registrations'),
('Mutual Fund Commission (Lumpsum)',  'Mutual Fund & Investment Income',    'Commission on lump-sum MF investments'),
('Mutual Fund Renewal / Trail Fee',   'Mutual Fund & Investment Income',    'Trail commission on existing MF folios'),
('Fixed Deposit Referral Income',     'Mutual Fund & Investment Income',    'Referral fee for FD scheme connections'),
('Bonds & Debenture Commission',      'Mutual Fund & Investment Income',    'Commission on bonds, NCDs, or debentures'),
('PMS / AIF Advisory Fee',            'Mutual Fund & Investment Income',    'Advisory fees for PMS clients'),
('NPS Subscription Commission',       'Mutual Fund & Investment Income',    'Commission from NPS subscriptions'),
('Demat / Trading Account Opening',   'Mutual Fund & Investment Income',    'Fee for opening demat/trading accounts'),
-- GROUP 4: Government & Registration Services (10)
('PAN Card Application Fee',          'Government & Registration Services', 'Service fee for PAN card applications'),
('Aadhaar Update / Correction Fee',   'Government & Registration Services', 'Fee for Aadhaar update/correction'),
('Passport Application Service Fee',  'Government & Registration Services', 'Fee for passport application/renewal'),
('Voter ID Service Fee',              'Government & Registration Services', 'Fee for voter ID application/correction'),
('Driving Licence Service Fee',       'Government & Registration Services', 'Fee for DL application/renewal'),
('GST Registration Fee',              'Government & Registration Services', 'Fee for GST registration/amendment'),
('ITR Filing Fee',                    'Government & Registration Services', 'Income tax return filing service charges'),
('Shop Act / Trade Licence Fee',      'Government & Registration Services', 'Fee for shop/trade licence'),
('Property Registration Assistance',  'Government & Registration Services', 'Fee for property registration support'),
('Firm / Company Registration Fee',   'Government & Registration Services', 'Fee for registering proprietorship/company'),
-- GROUP 5: Credit & CIBIL Services (5)
('CIBIL Score Check Fee',             'Credit & CIBIL Services',            'Fee for pulling and providing CIBIL report'),
('Credit Report Consultation Fee',    'Credit & CIBIL Services',            'Fee for analysing and explaining credit report'),
('CIBIL Dispute Resolution Fee',      'Credit & CIBIL Services',            'Fee for raising CIBIL disputes'),
('Credit Improvement Package Fee',    'Credit & CIBIL Services',            'Package fee for credit score enhancement'),
('CIBIL Subscription Plan Income',    'Credit & CIBIL Services',            'Monthly/annual CIBIL monitoring plan'),
-- GROUP 6: Consultancy Income (5)
('Financial Planning / Advisory Fee', 'Consultancy Income',                 'Fee for personalised financial planning'),
('Retirement Planning Fee',           'Consultancy Income',                 'Fee for retirement corpus planning'),
('Tax Consultancy Fee',               'Consultancy Income',                 'Tax planning and advisory service charges'),
('Investment Strategy Fee',           'Consultancy Income',                 'Fee for custom investment strategy'),
('Business Finance Consultancy',      'Consultancy Income',                 'Fee for business funding consultation'),
-- GROUP 7: Documentation & Processing Income (5)
('Document Collection Fee',           'Documentation & Processing Income',  'Fee for KYC document collection/verification'),
('File Processing Fee',               'Documentation & Processing Income',  'Internal processing fee per application'),
('Courier / Delivery Charges',        'Documentation & Processing Income',  'Courier charges recovered from clients'),
('Photocopy / Printing Charges',      'Documentation & Processing Income',  'Fee for photocopying/printing documents'),
('Notary / Attestation Fee',          'Documentation & Processing Income',  'Fee for notary/gazette/attestation'),
-- GROUP 8: Other Recurring Income (4)
('Account Maintenance Fee',           'Other Recurring Income',             'Annual/monthly account management charges'),
('SMS / Alert Service Charge',        'Other Recurring Income',             'Charges for SMS alert services'),
('Subscription / Membership Fee',     'Other Recurring Income',             'Income from CRM/tool subscription plans'),
('Renewal Reminder Service Fee',      'Other Recurring Income',             'Fee for proactive renewal reminders'),
-- GROUP 9: Office Side Income (3)
('Vendor Commission',                 'Office Side Income',                 'Commission from empanelled vendors'),
('Event / Seminar Registration Fee',  'Office Side Income',                 'Income from client events/workshops'),
('Penalty / Late Fee Recovery',       'Office Side Income',                 'Penalty recovered for late payments'),
-- GROUP 10: Passive Income (3)
('Interest Income',                   'Passive Income',                     'Interest earned on deposits or advances'),
('Dividend Income',                   'Passive Income',                     'Dividend received from company investments'),
('Rental Income',                     'Passive Income',                     'Income from renting office space or assets');


-- ── INCOME ENTRIES (15 records) ────────────────────────────
INSERT INTO income_entries (income_date, customer_id, income_category_id, amount, payment_mode, transaction_reference, remarks, employee_id, created_by)
SELECT CURDATE()-INTERVAL 3 DAY, c.id,
    (SELECT id FROM income_categories WHERE category_name='Home Loan Processing Fee'),
    12500.00,'BANK_TRANSFER','NEFT20260608001','Home Loan processing fee - Amit Verma (HDFC 25L)',
    (SELECT id FROM employees WHERE employee_id='EMP001'),(SELECT id FROM users WHERE username='admin')
FROM customers c WHERE c.mobile_number='9823001001';

INSERT INTO income_entries (income_date, customer_id, income_category_id, amount, payment_mode, transaction_reference, remarks, employee_id, created_by)
SELECT CURDATE()-INTERVAL 5 DAY, c.id,
    (SELECT id FROM income_categories WHERE category_name='Loan Against Property Fee'),
    18000.00,'CHEQUE','CHQ-RG-2026','LAP processing fee - Rajesh Gupta (Kotak 30L)',
    (SELECT id FROM employees WHERE employee_id='EMP001'),(SELECT id FROM users WHERE username='admin')
FROM customers c WHERE c.mobile_number='9823005001';

INSERT INTO income_entries (income_date, customer_id, income_category_id, amount, payment_mode, remarks, employee_id, created_by)
SELECT CURDATE()-INTERVAL 1 DAY, c.id,
    (SELECT id FROM income_categories WHERE category_name='CIBIL Score Check Fee'),
    500.00,'CASH','CIBIL report + consultation - Meena Deshpande',
    (SELECT id FROM employees WHERE employee_id='EMP001'),(SELECT id FROM users WHERE username='rahul.sharma')
FROM customers c WHERE c.mobile_number='9823010001';

INSERT INTO income_entries (income_date, customer_id, income_category_id, amount, payment_mode, transaction_reference, remarks, employee_id, created_by)
SELECT CURDATE(), c.id,
    (SELECT id FROM income_categories WHERE category_name='Term Insurance Commission'),
    3200.00,'UPI','UPI202606110001','LIC Tech Term commission - Rekha Jadhav (1Cr cover)',
    (SELECT id FROM employees WHERE employee_id='EMP004'),(SELECT id FROM users WHERE username='amit.joshi')
FROM customers c WHERE c.mobile_number='9823012001';

INSERT INTO income_entries (income_date, customer_id, income_category_id, amount, payment_mode, remarks, employee_id, created_by)
SELECT CURDATE()-INTERVAL 2 DAY, c.id,
    (SELECT id FROM income_categories WHERE category_name='Mutual Fund Commission (New SIP)'),
    1800.00,'UPI','SIP commission - Sunita Mahajan 5000/month Mirae',
    (SELECT id FROM employees WHERE employee_id='EMP004'),(SELECT id FROM users WHERE username='amit.joshi')
FROM customers c WHERE c.mobile_number='9823006001';

INSERT INTO income_entries (income_date, customer_id, income_category_id, amount, payment_mode, remarks, employee_id, created_by)
SELECT CURDATE()-INTERVAL 7 DAY, c.id,
    (SELECT id FROM income_categories WHERE category_name='ITR Filing Fee'),
    2000.00,'CASH','ITR FY 2025-26 filing - Suresh Patel (business income)',
    (SELECT id FROM employees WHERE employee_id='EMP002'),(SELECT id FROM users WHERE username='receptionist1')
FROM customers c WHERE c.mobile_number='9823003001';

INSERT INTO income_entries (income_date, customer_id, income_category_id, amount, payment_mode, transaction_reference, remarks, employee_id, created_by)
SELECT CURDATE()-INTERVAL 10 DAY, c.id,
    (SELECT id FROM income_categories WHERE category_name='Personal Loan Processing Fee'),
    5000.00,'UPI','UPI202606010099','Personal Loan processing - Priya Singh (5L)',
    (SELECT id FROM employees WHERE employee_id='EMP003'),(SELECT id FROM users WHERE username='priya.patil')
FROM customers c WHERE c.mobile_number='9823002001';

INSERT INTO income_entries (income_date, customer_id, income_category_id, amount, payment_mode, transaction_reference, remarks, employee_id, created_by)
SELECT CURDATE()-INTERVAL 4 DAY, c.id,
    (SELECT id FROM income_categories WHERE category_name='Car / Vehicle Loan Processing Fee'),
    6500.00,'BANK_TRANSFER','RTGS202606070088','Car Loan processing - Deepak Thakur (SBI 6.5L Swift)',
    (SELECT id FROM employees WHERE employee_id='EMP003'),(SELECT id FROM users WHERE username='priya.patil')
FROM customers c WHERE c.mobile_number='9823007001';

INSERT INTO income_entries (income_date, customer_id, income_category_id, amount, payment_mode, remarks, employee_id, created_by)
SELECT CURDATE()-INTERVAL 8 DAY, c.id,
    (SELECT id FROM income_categories WHERE category_name='Health Insurance Commission'),
    4200.00,'UPI','Star Health family floater 5L - Anita Kulkarni',
    (SELECT id FROM employees WHERE employee_id='EMP004'),(SELECT id FROM users WHERE username='amit.joshi')
FROM customers c WHERE c.mobile_number='9823004001';

INSERT INTO income_entries (income_date, customer_id, income_category_id, amount, payment_mode, remarks, created_by)
SELECT CURDATE()-INTERVAL 6 DAY, c.id,
    (SELECT id FROM income_categories WHERE category_name='Document Collection Fee'),
    300.00,'CASH','KYC document collection charges - Kavita Sawant',
    (SELECT id FROM users WHERE username='receptionist1')
FROM customers c WHERE c.mobile_number='9823008001';

INSERT INTO income_entries (income_date, income_category_id, amount, payment_mode, remarks, created_by)
SELECT CURDATE()-INTERVAL 9 DAY,
    (SELECT id FROM income_categories WHERE category_name='PAN Card Application Fee'),
    150.00,'CASH','New PAN card - walk-in customer',
    (SELECT id FROM users WHERE username='receptionist1');

INSERT INTO income_entries (income_date, customer_id, income_category_id, amount, payment_mode, transaction_reference, remarks, employee_id, created_by)
SELECT CURDATE()-INTERVAL 12 DAY, c.id,
    (SELECT id FROM income_categories WHERE category_name='DSA / Referral Commission'),
    22000.00,'BANK_TRANSFER','HDFC-DSA-2026-0089','HDFC DSA commission - Nilesh Borse home loan 18L approved',
    (SELECT id FROM employees WHERE employee_id='EMP001'),(SELECT id FROM users WHERE username='admin')
FROM customers c WHERE c.mobile_number='9823009001';

INSERT INTO income_entries (income_date, income_category_id, amount, payment_mode, remarks, created_by)
SELECT CURDATE()-INTERVAL 14 DAY,
    (SELECT id FROM income_categories WHERE category_name='GST Registration Fee'),
    1500.00,'UPI','New GST registration - new business client',
    (SELECT id FROM users WHERE username='receptionist1');

INSERT INTO income_entries (income_date, income_category_id, amount, payment_mode, remarks, created_by)
SELECT CURDATE()-INTERVAL 16 DAY,
    (SELECT id FROM income_categories WHERE category_name='Courier / Delivery Charges'),
    250.00,'CASH','Courier charges recovered - 3 files sent to Pune bank',
    (SELECT id FROM users WHERE username='receptionist1');

INSERT INTO income_entries (income_date, income_category_id, amount, payment_mode, transaction_reference, remarks, created_by)
SELECT CURDATE()-INTERVAL 30 DAY,
    (SELECT id FROM income_categories WHERE category_name='Interest Income'),
    8750.00,'BANK_TRANSFER','HDFC-FD-INT-2026-05','Interest received on office FD 5L (May 2026)',
    (SELECT id FROM users WHERE username='admin');

-- ── Additional test data for Insurance Income (Dashboard Chart 2) ────
INSERT INTO income_entries (income_date, income_category_id, amount, payment_mode, remarks, created_by)
SELECT CURDATE()-INTERVAL 2 DAY,
    (SELECT id FROM income_categories WHERE category_name='Life Insurance Premium Commission' LIMIT 1),
    5500.00, 'UPI', 'Life Insurance commission - Test entry', (SELECT id FROM users WHERE username='admin' LIMIT 1);

INSERT INTO income_entries (income_date, income_category_id, amount, payment_mode, remarks, created_by)
SELECT CURDATE()-INTERVAL 1 DAY,
    (SELECT id FROM income_categories WHERE category_name='Health Insurance Commission' LIMIT 1),
    3200.00, 'BANK_TRANSFER', 'Health Insurance commission - Test entry', (SELECT id FROM users WHERE username='admin' LIMIT 1);

INSERT INTO income_entries (income_date, income_category_id, amount, payment_mode, remarks, created_by)
SELECT CURDATE(),
    (SELECT id FROM income_categories WHERE category_name='Term Insurance Commission' LIMIT 1),
    4100.00, 'UPI', 'Term Insurance commission - Test entry', (SELECT id FROM users WHERE username='admin' LIMIT 1);

INSERT INTO income_entries (income_date, income_category_id, amount, payment_mode, remarks, created_by)
SELECT CURDATE()-INTERVAL 5 DAY,
    (SELECT id FROM income_categories WHERE category_name='General Insurance Commission' LIMIT 1),
    2800.00, 'CASH', 'General Insurance commission - Test entry', (SELECT id FROM users WHERE username='admin' LIMIT 1);

-- ── Additional test data for Mutual Fund Income (Dashboard Chart 3) ────
INSERT INTO income_entries (income_date, income_category_id, amount, payment_mode, remarks, created_by)
SELECT CURDATE()-INTERVAL 3 DAY,
    (SELECT id FROM income_categories WHERE category_name='Mutual Fund Commission (New SIP)' LIMIT 1),
    2500.00, 'BANK_TRANSFER', 'MF SIP commission - Test entry', (SELECT id FROM users WHERE username='admin' LIMIT 1);

INSERT INTO income_entries (income_date, income_category_id, amount, payment_mode, remarks, created_by)
SELECT CURDATE()-INTERVAL 2 DAY,
    (SELECT id FROM income_categories WHERE category_name='Mutual Fund Commission (Lumpsum)' LIMIT 1),
    6800.00, 'UPI', 'MF Lumpsum commission - Test entry', (SELECT id FROM users WHERE username='admin' LIMIT 1);

INSERT INTO income_entries (income_date, income_category_id, amount, payment_mode, remarks, created_by)
SELECT CURDATE()-INTERVAL 4 DAY,
    (SELECT id FROM income_categories WHERE category_name='Mutual Fund Renewal / Trail Fee' LIMIT 1),
    1800.00, 'CASH', 'MF Trail commission - Test entry', (SELECT id FROM users WHERE username='admin' LIMIT 1);

INSERT INTO income_entries (income_date, income_category_id, amount, payment_mode, remarks, created_by)
SELECT CURDATE()-INTERVAL 6 DAY,
    (SELECT id FROM income_categories WHERE category_name='Fixed Deposit Referral Income' LIMIT 1),
    3500.00, 'BANK_TRANSFER', 'FD Referral income - Test entry', (SELECT id FROM users WHERE username='admin' LIMIT 1);


-- ── INCOME (seed data for dashboard — 'income' table) ──────
-- These records satisfy Income.java (@Table name = "income")
-- and fix the DashboardController monthlyIncome queries
INSERT INTO income (income_date, category_id, customer_id, inquiry_id, amount, payment_mode, description, created_by)
SELECT CURDATE()-INTERVAL 3 DAY,
    (SELECT id FROM income_categories WHERE category_name='Home Loan Processing Fee'),
    (SELECT id FROM customers WHERE mobile_number='9823001001'),
    (SELECT i.id FROM inquiries i JOIN customers c ON c.id=i.customer_id WHERE c.mobile_number='9823001001' LIMIT 1),
    12500.00,'UPI','Home Loan fee - Amit Verma',
    (SELECT id FROM users WHERE username='admin');

INSERT INTO income (income_date, category_id, customer_id, amount, payment_mode, description, created_by)
SELECT CURDATE()-INTERVAL 5 DAY,
    (SELECT id FROM income_categories WHERE category_name='Loan Against Property Fee'),
    (SELECT id FROM customers WHERE mobile_number='9823005001'),
    18000.00,'CHEQUE','LAP fee - Rajesh Gupta',
    (SELECT id FROM users WHERE username='admin');

INSERT INTO income (income_date, category_id, customer_id, amount, payment_mode, description, created_by)
SELECT CURDATE()-INTERVAL 1 DAY,
    (SELECT id FROM income_categories WHERE category_name='CIBIL Score Check Fee'),
    (SELECT id FROM customers WHERE mobile_number='9823010001'),
    500.00,'CASH','CIBIL report - Meena Deshpande',
    (SELECT id FROM users WHERE username='admin');

INSERT INTO income (income_date, category_id, customer_id, amount, payment_mode, description, created_by)
SELECT CURDATE(),
    (SELECT id FROM income_categories WHERE category_name='Term Insurance Commission'),
    (SELECT id FROM customers WHERE mobile_number='9823012001'),
    3200.00,'UPI','Term Insurance commission - Rekha Jadhav',
    (SELECT id FROM users WHERE username='admin');

INSERT INTO income (income_date, category_id, customer_id, amount, payment_mode, description, created_by)
SELECT CURDATE()-INTERVAL 2 DAY,
    (SELECT id FROM income_categories WHERE category_name='Mutual Fund Commission (New SIP)'),
    (SELECT id FROM customers WHERE mobile_number='9823006001'),
    1800.00,'UPI','SIP commission - Sunita Mahajan',
    (SELECT id FROM users WHERE username='admin');

INSERT INTO income (income_date, category_id, customer_id, amount, payment_mode, description, created_by)
SELECT CURDATE()-INTERVAL 7 DAY,
    (SELECT id FROM income_categories WHERE category_name='ITR Filing Fee'),
    (SELECT id FROM customers WHERE mobile_number='9823003001'),
    2000.00,'CASH','ITR filing - Suresh Patel',
    (SELECT id FROM users WHERE username='admin');

INSERT INTO income (income_date, category_id, customer_id, amount, payment_mode, description, created_by)
SELECT CURDATE()-INTERVAL 10 DAY,
    (SELECT id FROM income_categories WHERE category_name='Personal Loan Processing Fee'),
    (SELECT id FROM customers WHERE mobile_number='9823002001'),
    5000.00,'UPI','Personal Loan fee - Priya Singh',
    (SELECT id FROM users WHERE username='admin');

INSERT INTO income (income_date, category_id, customer_id, amount, payment_mode, description, created_by)
SELECT CURDATE()-INTERVAL 4 DAY,
    (SELECT id FROM income_categories WHERE category_name='Car / Vehicle Loan Processing Fee'),
    (SELECT id FROM customers WHERE mobile_number='9823007001'),
    6500.00,'CASH','Car Loan fee - Deepak Thakur',
    (SELECT id FROM users WHERE username='admin');

INSERT INTO income (income_date, category_id, customer_id, amount, payment_mode, description, created_by)
SELECT CURDATE()-INTERVAL 8 DAY,
    (SELECT id FROM income_categories WHERE category_name='Health Insurance Commission'),
    (SELECT id FROM customers WHERE mobile_number='9823004001'),
    4200.00,'UPI','Health insurance - Anita Kulkarni',
    (SELECT id FROM users WHERE username='admin');

INSERT INTO income (income_date, category_id, amount, payment_mode, description, created_by)
SELECT CURDATE()-INTERVAL 12 DAY,
    (SELECT id FROM income_categories WHERE category_name='DSA / Referral Commission'),
    22000.00,'CASH','HDFC DSA commission - Nilesh Borse',
    (SELECT id FROM users WHERE username='admin');

INSERT INTO income (income_date, category_id, amount, payment_mode, description, created_by)
SELECT CURDATE()-INTERVAL 30 DAY,
    (SELECT id FROM income_categories WHERE category_name='Interest Income'),
    8750.00,'CASH','Office FD interest May 2026',
    (SELECT id FROM users WHERE username='admin');


-- ── NOTIFICATIONS (5 records) ──────────────────────────────
INSERT INTO notifications (user_id, title, message, notification_type, reference_type, is_read, scheduled_at)
SELECT u.id,'Follow-up Due Tomorrow',
    'You have a follow-up tomorrow with Priya Singh regarding Personal Loan documents.',
    'FOLLOWUP_REMINDER','FOLLOWUP',FALSE,NOW()
FROM users u WHERE u.username='priya.patil';

INSERT INTO notifications (user_id, title, message, notification_type, reference_type, is_read, scheduled_at)
SELECT u.id,'Urgent Follow-up - Rajesh Gupta',
    'LAP case for Rajesh Gupta is in FOLLOW_UP status. Legal opinion still pending.',
    'FOLLOWUP_REMINDER','FOLLOWUP',FALSE,NOW()
FROM users u WHERE u.username='rahul.sharma';

INSERT INTO notifications (user_id, title, message, notification_type, reference_type, is_read, scheduled_at)
SELECT u.id,'Customer Birthday Today',
    'Customer Anita Kulkarni celebrates her birthday today! Consider sending wishes.',
    'BIRTHDAY_REMINDER','CUSTOMER',FALSE,NOW()
FROM users u WHERE u.username='admin';

INSERT INTO notifications (user_id, title, message, notification_type, reference_type, is_read, scheduled_at)
SELECT u.id,'New Inquiry Assigned',
    'Education loan inquiry for Kavita Sawant has been assigned to you.',
    'GENERAL','INQUIRY',FALSE,NOW()
FROM users u WHERE u.username='rahul.sharma';

INSERT INTO notifications (user_id, title, message, notification_type, reference_type, is_read, scheduled_at)
SELECT u.id,'Appointment Reminder',
    'Document collection appointment with Amit Verma is in 3 days.',
    'APPOINTMENT_REMINDER','APPOINTMENT',FALSE,NOW()
FROM users u WHERE u.username='rahul.sharma';


-- ── AUDIT LOGS (sample) ────────────────────────────────────
INSERT INTO audit_logs (user_id, username, action, entity_type, entity_id, ip_address, status, created_at)
VALUES
((SELECT id FROM users WHERE username='admin'),        'admin',        'USER_LOGIN',          'User',        1,'192.168.1.10','SUCCESS',NOW()-INTERVAL 2 HOUR),
((SELECT id FROM users WHERE username='admin'),        'admin',        'CREATE_CUSTOMER',      'Customer',    1,'192.168.1.10','SUCCESS',NOW()-INTERVAL 1 HOUR),
((SELECT id FROM users WHERE username='rahul.sharma'), 'rahul.sharma', 'USER_LOGIN',          'User',        3,'192.168.1.15','SUCCESS',NOW()-INTERVAL 3 HOUR),
((SELECT id FROM users WHERE username='rahul.sharma'), 'rahul.sharma', 'CREATE_INCOME_ENTRY', 'IncomeEntry', 1,'192.168.1.15','SUCCESS',NOW()-INTERVAL 30 MINUTE),
((SELECT id FROM users WHERE username='admin'),        'admin',        'CREATE_EXPENSE',       'Expense',     1,'192.168.1.10','SUCCESS',NOW()-INTERVAL 45 MINUTE);


-- ============================================================
-- VERIFICATION (uncomment to run after import)
-- ============================================================
-- SELECT 'roles'              tbl, COUNT(*) cnt FROM roles
-- UNION SELECT 'users',             COUNT(*) FROM users
-- UNION SELECT 'employees',         COUNT(*) FROM employees
-- UNION SELECT 'customers',         COUNT(*) FROM customers
-- UNION SELECT 'inquiry_types',     COUNT(*) FROM inquiry_types
-- UNION SELECT 'inquiries',         COUNT(*) FROM inquiries
-- UNION SELECT 'followups',         COUNT(*) FROM followups
-- UNION SELECT 'appointments',      COUNT(*) FROM appointments
-- UNION SELECT 'attendance',        COUNT(*) FROM attendance
-- UNION SELECT 'employee_targets',  COUNT(*) FROM employee_targets
-- UNION SELECT 'employee_daily_work',COUNT(*) FROM employee_daily_work
-- UNION SELECT 'expense_categories',COUNT(*) FROM expense_categories
-- UNION SELECT 'expenses',          COUNT(*) FROM expenses
-- UNION SELECT 'income_categories', COUNT(*) FROM income_categories
-- UNION SELECT 'income_entries',    COUNT(*) FROM income_entries
-- UNION SELECT 'income',            COUNT(*) FROM income
-- UNION SELECT 'notifications',     COUNT(*) FROM notifications
-- UNION SELECT 'audit_logs',        COUNT(*) FROM audit_logs;

-- ============================================================
-- END OF SCRIPT
-- ============================================================
