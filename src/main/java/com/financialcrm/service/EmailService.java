package com.financialcrm.service;

public interface EmailService {

    boolean sendHtmlEmail(String toEmail, String subject, String htmlBody);

    boolean sendBirthdayEmail(String customerName, String toEmail);

    boolean sendAnniversaryEmail(String customerName, String toEmail);
}