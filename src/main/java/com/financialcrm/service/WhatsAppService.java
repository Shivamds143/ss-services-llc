package com.financialcrm.service;

public interface WhatsAppService {
    /**
     * Send a WhatsApp message to a mobile number.
     * @param mobileNumber 10-digit Indian mobile number (e.g. "9876543210")
     * @param message      The message text to send
     * @return true if sent successfully, false otherwise
     */
    boolean sendMessage(String mobileNumber, String message);

    /**
     * Send birthday wishes via WhatsApp.
     * @param customerName   Customer's full name
     * @param mobileNumber   10-digit mobile number
     */
    boolean sendBirthdayWishes(String customerName, String mobileNumber);

    /**
     * Send anniversary wishes via WhatsApp.
     * @param customerName   Customer's full name
     * @param mobileNumber   10-digit mobile number
     */
    boolean sendAnniversaryWishes(String customerName, String mobileNumber);
}