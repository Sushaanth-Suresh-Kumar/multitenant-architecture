package dev.sushaanth.bookly.security.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String emailFrom;

    @Value("${application.base-url}")
    private String baseUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtp(String to, String otp) {
        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("otp", otp);

        sendEmail(
                to,
                "Bookly - Email Verification",
                "email-templates/otp-email.html",
                templateModel
        );
    }

    public void sendEmployeeInvitation(String to, String adminName, String libraryName, String invitationId) {
        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("adminName", adminName);
        templateModel.put("libraryName", libraryName);
        templateModel.put("invitationUrl", baseUrl + "/register/employee?invitation=" + invitationId);

        sendEmail(
                to,
                "Invitation to join " + libraryName + " on Bookly",
                "email-templates/invitation-email.html",
                templateModel
        );
    }

    private void sendEmail(String to, String subject, String templatePath, Map<String, Object> model) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(emailFrom);
            helper.setTo(to);
            helper.setSubject(subject);

            String template = loadTemplate(templatePath);
            String content = processTemplate(template, model);

            helper.setText(content, true);
            mailSender.send(message);
        } catch (MessagingException | IOException e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private String loadTemplate(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        try (InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        }
    }

    private String processTemplate(String template, Map<String, Object> model) {
        String result = template;
        for (Map.Entry<String, Object> entry : model.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", entry.getValue().toString());
        }
        return result;
    }
}