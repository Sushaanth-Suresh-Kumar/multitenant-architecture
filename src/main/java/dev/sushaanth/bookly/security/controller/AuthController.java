package dev.sushaanth.bookly.security.controller;

import dev.sushaanth.bookly.security.JwtTokenUtil;
import dev.sushaanth.bookly.security.dto.JwtResponse;
import dev.sushaanth.bookly.security.dto.LoginRequest;
import dev.sushaanth.bookly.security.dto.SignupRequest;
import dev.sushaanth.bookly.security.model.LibraryUser;
import dev.sushaanth.bookly.security.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        JwtResponse jwtResponse = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(jwtResponse);
    }

    @PostMapping("/register/library")
    public ResponseEntity<?> registerLibrary(@Valid @RequestBody SignupRequest signupRequest) {
        authService.registerLibrary(signupRequest);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/register/employee")
    public ResponseEntity<?> registerEmployee(@Valid @RequestBody SignupRequest signupRequest) {
        authService.registerEmployee(signupRequest);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}