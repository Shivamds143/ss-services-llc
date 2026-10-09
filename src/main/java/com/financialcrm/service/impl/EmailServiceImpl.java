package com.financialcrm.service.impl;

import com.financialcrm.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.company:Financial CRM}")
    private String companyName;

    @Override
    public boolean sendHtmlEmail(String toEmail, String subject, String htmlBody) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail, companyName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(mimeMessage);
            log.info("Email sent successfully to {} | Subject: {}", toEmail, subject);
            return true;

        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean sendBirthdayEmail(String customerName, String toEmail) {
        String subject = "🎂 Happy Birthday, " + customerName + "!";
        String htmlBody = buildBirthdayEmailHtml(customerName);
        return sendHtmlEmail(toEmail, subject, htmlBody);
    }

    @Override
    public boolean sendAnniversaryEmail(String customerName, String toEmail) {
        String subject = "💍 Happy Anniversary, " + customerName + "!";
        String htmlBody = buildAnniversaryEmailHtml(customerName);
        return sendHtmlEmail(toEmail, subject, htmlBody);
    }

    private String buildBirthdayEmailHtml(String customerName) {
        return "<!DOCTYPE html>"
                + "<html><body style='margin:0;padding:0;background-color:#f4f6f8;font-family:Arial,Helvetica,sans-serif;'>"
                + "<table width='100%' cellpadding='0' cellspacing='0' style='background-color:#f4f6f8;padding:30px 0;'>"
                + "<tr><td align='center'>"
                + "<table width='500' cellpadding='0' cellspacing='0' style='background-color:#ffffff;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.08);'>"
                + "<tr><td style='background-color:#7b2ff7;padding:24px;text-align:center;'>"
                + "<h1 style='color:#ffffff;margin:0;font-size:22px;'>&#127881; Happy Birthday!</h1>"
                + "</td></tr>"
                + "<tr><td style='padding:28px 30px;color:#333333;'>"
                + "<p style='font-size:16px;margin:0 0 16px;'>Dear <strong>" + customerName + "</strong>,</p>"
                + "<p style='font-size:15px;line-height:1.6;margin:0 0 16px;'>"
                + "Wishing you a very Happy Birthday! May your special day be filled with joy, good health "
                + "and happiness, and may the year ahead bring you success and prosperity."
                + "</p>"
                + "<p style='font-size:15px;line-height:1.6;margin:0 0 16px;'>"
                + "Thank you for being a valued part of our family."
                + "</p>"
                + "<p style='font-size:15px;margin:24px 0 0;'>Warm regards,<br><strong>" + companyName + " Team</strong></p>"
                + "</td></tr>"
                + "<tr><td style='background-color:#f0f0f5;padding:14px;text-align:center;font-size:12px;color:#999999;'>"
                + "This is an automated message, please do not reply."
                + "</td></tr>"
                + "</table></td></tr></table></body></html>";
    }

    private String buildAnniversaryEmailHtml(String customerName) {
        return "<!DOCTYPE html>"
                + "<html><body style='margin:0;padding:0;background-color:#f4f6f8;font-family:Arial,Helvetica,sans-serif;'>"
                + "<table width='100%' cellpadding='0' cellspacing='0' style='background-color:#f4f6f8;padding:30px 0;'>"
                + "<tr><td align='center'>"
                + "<table width='500' cellpadding='0' cellspacing='0' style='background-color:#ffffff;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.08);'>"
                + "<tr><td style='background-color:#e0578a;padding:24px;text-align:center;'>"
                + "<h1 style='color:#ffffff;margin:0;font-size:22px;'>&#128141; Happy Anniversary!</h1>"
                + "</td></tr>"
                + "<tr><td style='padding:28px 30px;color:#333333;'>"
                + "<p style='font-size:16px;margin:0 0 16px;'>Dear <strong>" + customerName + "</strong>,</p>"
                + "<p style='font-size:15px;line-height:1.6;margin:0 0 16px;'>"
                + "Congratulations on your special day! Wishing you and your family many more years of "
                + "love, happiness and togetherness."
                + "</p>"
                + "<p style='font-size:15px;line-height:1.6;margin:0 0 16px;'>"
                + "Thank you for being a valued part of our family."
                + "</p>"
                + "<p style='font-size:15px;margin:24px 0 0;'>Warm regards,<br><strong>" + companyName + " Team</strong></p>"
                + "</td></tr>"
                + "<tr><td style='background-color:#f0f0f5;padding:14px;text-align:center;font-size:12px;color:#999999;'>"
                + "This is an automated message, please do not reply."
                + "</td></tr>"
                + "</table></td></tr></table></body></html>";
    }
}