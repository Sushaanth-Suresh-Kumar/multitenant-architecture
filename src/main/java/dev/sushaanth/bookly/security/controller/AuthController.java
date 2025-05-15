// 7. Updated AuthController
package dev.sushaanth.bookly.security.controller;

import dev.sushaanth.bookly.security.dto.JwtResponse;
import dev.sushaanth.bookly.security.dto.LoginRequest;
import dev.sushaanth.bookly.security.dto.RegistrationRequest;
import dev.sushaanth.bookly.security.dto.ResendOtpRequest;
import dev.sushaanth.bookly.security.dto.VerificationRequest;
import dev.sushaanth.bookly.security.exception.InvalidOtpException;
import dev.sushaanth.bookly.security.exception.OtpAlreadyUsedException;
import dev.sushaanth.bookly.security.exception.OtpExpiredException;
import dev.sushaanth.bookly.security.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        JwtResponse jwtResponse = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(jwtResponse);
    }

    @PostMapping("/register/library")
    public ResponseEntity<Void> initiateLibraryRegistration(@Valid @RequestBody RegistrationRequest request) {
        authService.initiateLibraryRegistration(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/register/employee")
    public ResponseEntity<Void> initiateEmployeeRegistration(@Valid @RequestBody RegistrationRequest request) {
        // Similar implementation to library registration
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/verify")
    public ResponseEntity<Void> verifyRegistration(@Valid @RequestBody VerificationRequest request) {
        try {
            authService.verifyAndCompleteRegistration(request);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (InvalidOtpException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (OtpExpiredException e) {
            throw new ResponseStatusException(HttpStatus.GONE, e.getMessage());
        } catch (OtpAlreadyUsedException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<Void> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        authService.resendOtp(request.email());
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}