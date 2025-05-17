package dev.sushaanth.bookly.security.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtp(String to, String otp) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            String htmlMsg = "<div style='font-family: Arial, sans-serif; padding: 20px; max-width: 600px;'>"
                    + "<h2 style='color: #4285f4;'>Bookly - Email Verification</h2>"
                    + "<p>Thank you for registering with Bookly. Please use the following OTP to verify your email address:</p>"
                    + "<div style='background-color: #f0f0f0; padding: 15px; text-align: center; font-size: 24px; letter-spacing: 5px; margin: 20px 0;'>"
                    + "<strong>" + otp + "</strong>"
                    + "</div>"
                    + "<p>This OTP is valid for 10 minutes.</p>"
                    + "<p>If you did not request this verification, please ignore this email.</p>"
                    + "</div>";

            helper.setTo(to);
            helper.setSubject("Bookly - Email Verification");
            helper.setText(htmlMsg, true);

            mailSender.send(mimeMessage);
            logger.info("OTP email sent to {}", to);
        } catch (MessagingException e) {
            logger.error("Failed to send OTP email", e);
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }
}