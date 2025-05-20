package dev.sushaanth.bookly.security.controller;

import dev.sushaanth.bookly.security.dto.JwtResponse;
import dev.sushaanth.bookly.security.dto.LoginRequest;
import dev.sushaanth.bookly.security.service.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        JwtResponse response = authenticationService.authenticateUser(loginRequest);
        return ResponseEntity.ok(response);
    }
}