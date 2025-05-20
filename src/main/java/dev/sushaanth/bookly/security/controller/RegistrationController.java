package dev.sushaanth.bookly.security.controller;

import dev.sushaanth.bookly.security.dto.RegistrationRequest;
import dev.sushaanth.bookly.security.dto.ResendOtpRequest;
import dev.sushaanth.bookly.security.dto.VerificationRequest;
import dev.sushaanth.bookly.security.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/registration")
public class RegistrationController {
    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/library")
    public ResponseEntity<Void> initiateLibraryRegistration(@Valid @RequestBody RegistrationRequest request) {
        registrationService.initiateLibraryRegistration(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/employee")
    public ResponseEntity<Void> initiateEmployeeRegistration(@Valid @RequestBody RegistrationRequest request) {
        registrationService.initiateEmployeeRegistration(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/verify")
    public ResponseEntity<Void> verifyRegistration(@Valid @RequestBody VerificationRequest request) {
        registrationService.verifyAndCompleteRegistration(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<Void> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        registrationService.resendOtp(request.email());
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
