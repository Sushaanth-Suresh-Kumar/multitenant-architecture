package dev.sushaanth.bookly.security.controller;

import dev.sushaanth.bookly.security.dto.*;
import dev.sushaanth.bookly.security.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/register")
public class RegistrationController {
    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/init")
    public ResponseEntity<EmailVerificationResponse> initiateRegistration(
            @Valid @RequestBody InitialRegistrationRequest request) {
        EmailVerificationResponse response = registrationService.initiateEmailVerification(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<EmailVerificationResult> verifyEmail(
            @Valid @RequestBody VerificationRequest request) {
        EmailVerificationResult result = registrationService.verifyEmail(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/complete")
    public ResponseEntity<RegistrationResponse> completeRegistration(
            @Valid @RequestBody CompleteRegistrationRequest request) {
        RegistrationResponse response = registrationService.completeRegistration(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<EmailVerificationResponse> resendOtp(
            @Valid @RequestBody ResendOtpRequest request) {
        EmailVerificationResponse response = registrationService.resendOtp(request.email());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}