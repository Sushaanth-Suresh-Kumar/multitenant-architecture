package dev.sushaanth.bookly.security.service;

import dev.sushaanth.bookly.exception.BooklyException;
import dev.sushaanth.bookly.exception.BooklyException.ErrorCode;
import dev.sushaanth.bookly.security.dto.JwtResponse;
import dev.sushaanth.bookly.security.dto.LoginRequest;
import dev.sushaanth.bookly.security.jwt.JwtTokenUtil;
import dev.sushaanth.bookly.security.model.LibraryUser;
import dev.sushaanth.bookly.security.repository.LibraryUserRepository;
import dev.sushaanth.bookly.tenant.Tenant;
import dev.sushaanth.bookly.tenant.TenantRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {
    private final AuthenticationManager authenticationManager;
    private final LibraryUserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final JwtTokenUtil jwtTokenUtil;

    public AuthenticationService(AuthenticationManager authenticationManager,
                                 LibraryUserRepository userRepository,
                                 TenantRepository tenantRepository,
                                 JwtTokenUtil jwtTokenUtil) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.username(),
                        loginRequest.password()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        LibraryUser user = userRepository.findByUsername(loginRequest.username())
                .orElseThrow(() -> new BooklyException(ErrorCode.INVALID_CREDENTIALS, "User not found"));

        Tenant tenant = tenantRepository.findById(user.getTenantId())
                .orElseThrow(() -> new BooklyException(ErrorCode.TENANT_NOT_FOUND, "Tenant not found"));

        String jwt = jwtTokenUtil.generateToken(
                user.getUsername(),
                user.getId(),  // Pass user ID
                user.getTenantId(),
                tenant.getSchemaName(),
                user.getRole()
        );

        return new JwtResponse(jwt, user.getUsername(), user.getRole().name());
    }
}