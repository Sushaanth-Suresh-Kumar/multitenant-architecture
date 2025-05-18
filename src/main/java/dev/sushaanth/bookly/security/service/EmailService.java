package dev.sushaanth.bookly.security.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${application.base-url}")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String emailUser;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;

        if (mailSender instanceof JavaMailSenderImpl mailSenderImpl) {
            logger.info("Mail configuration: host={}, port={}, username={}",
                    mailSenderImpl.getHost(), mailSenderImpl.getPort(), mailSenderImpl.getUsername());
            logger.info("Mail properties: {}", mailSenderImpl.getJavaMailProperties());
        }
    }

    public void sendOtp(String to, String otp) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            helper.setFrom(emailUser);

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

    /**
     * Sends an invitation email to a potential employee.
     *
     * @param to The email address of the recipient
     * @param adminName The username of the admin who sent the invitation
     * @param libraryName The name of the library (tenant)
     * @param invitationId The ID of the invitation (used for verification)
     */
    public void sendEmployeeInvitation(String to, String adminName, String libraryName, String invitationId) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            helper.setFrom(emailUser);

            // Create an invitation URL with the invitation ID
            String invitationUrl = baseUrl + "/register/employee?invitationId=" + invitationId;

            String htmlMsg = "<div style='font-family: Arial, sans-serif; padding: 20px; max-width: 600px;'>"
                    + "<h2 style='color: #4285f4;'>Bookly - Join " + libraryName + "</h2>"
                    + "<p>Hello,</p>"
                    + "<p>You have been invited to join <strong>" + libraryName + "</strong> as an employee by <strong>" + adminName + "</strong>.</p>"
                    + "<div style='background-color: #f0f0f0; padding: 15px; margin: 20px 0;'>"
                    + "<p>Click the button below to complete your registration:</p>"
                    + "<div style='text-align: center;'>"
                    + "<a href='" + invitationUrl + "' style='display: inline-block; background-color: #4285f4; color: white; padding: 10px 20px; text-decoration: none; border-radius: 4px; font-weight: bold;'>Accept Invitation</a>"
                    + "</div>"
                    + "</div>"
                    + "<p>Alternatively, you can copy and paste the following link into your browser:</p>"
                    + "<p style='word-break: break-all;'><small>" + invitationUrl + "</small></p>"
                    + "<p><strong>Note:</strong> This invitation expires in 7 days.</p>"
                    + "<p>If you did not expect this invitation, please disregard this email.</p>"
                    + "<hr style='border: 1px solid #eee; margin: 20px 0;'>"
                    + "<p style='color: #666; font-size: 12px;'>Bookly - Library Management System</p>"
                    + "</div>";

            helper.setTo(to);
            helper.setSubject("Invitation to join " + libraryName + " on Bookly");
            helper.setText(htmlMsg, true);

            mailSender.send(mimeMessage);
            logger.info("Invitation email sent to {}", to);
        } catch (MessagingException e) {
            logger.error("Failed to send invitation email", e);
            throw new RuntimeException("Failed to send invitation email", e);
        }
    }
}