-- Migration: Add insurance fields to inquiries table
-- Run this script on your MySQL database if the columns don't already exist

ALTER TABLE inquiries
    ADD COLUMN IF NOT EXISTS insurance_type     VARCHAR(50)  NULL
        COMMENT 'Life Insurance | Health Insurance | General Insurance'
        AFTER rejection_reason,
    ADD COLUMN IF NOT EXISTS insurance_sub_type VARCHAR(150) NULL
        COMMENT 'Custom sub-type e.g. Motor, Term, Travel, Fire'
        AFTER insurance_type;
