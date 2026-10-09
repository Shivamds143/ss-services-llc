package com.financialcrm.service.impl;

import com.financialcrm.service.WhatsAppService;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class WhatsAppServiceImpl implements WhatsAppService {

    @Value("${twilio.whatsapp.from}")
    private String fromNumber;

    @Override
    public boolean sendMessage(String mobileNumber, String messageText) {
        try {
            // Format Indian number to international format for WhatsApp
            String toNumber = "whatsapp:+91" + mobileNumber.trim();

            Message message = Message.creator(
                    new PhoneNumber(toNumber),
                    new PhoneNumber(fromNumber),
                    messageText
            ).create();

            log.info("WhatsApp message sent to {} | SID: {} | Status: {}",
                    mobileNumber, message.getSid(), message.getStatus());
            return true;

        } catch (Exception e) {
            log.error("Failed to send WhatsApp message to {}: {}", mobileNumber, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean sendBirthdayWishes(String customerName, String mobileNumber) {
        String message = buildBirthdayMessage(customerName);
        return sendMessage(mobileNumber, message);
    }

    @Override
    public boolean sendAnniversaryWishes(String customerName, String mobileNumber) {
        String message = buildAnniversaryMessage(customerName);
        return sendMessage(mobileNumber, message);
    }

    private String buildBirthdayMessage(String customerName) {
        return "🎂 *Happy Birthday, " + customerName + "!* 🎉\n\n" +
               "Wishing you a wonderful day filled with joy and happiness! " +
               "May this special day bring you all that your heart desires. 🌟\n\n" +
               "Warm regards,\n" +
               "*Financial CRM Team*";
    }

    private String buildAnniversaryMessage(String customerName) {
        return "💍 *Happy Anniversary, " + customerName + "!* 🎊\n\n" +
               "Congratulations on this beautiful milestone! " +
               "Wishing you many more years of love, laughter, and happiness together. ❤️\n\n" +
               "Warm regards,\n" +
               "*Financial CRM Team*";
    }
}