package dev.sushaanth.bookly.security.service;

import dev.sushaanth.bookly.security.JwtTokenUtil;
import dev.sushaanth.bookly.security.dto.JwtResponse;
import dev.sushaanth.bookly.security.dto.LoginRequest;
import dev.sushaanth.bookly.security.dto.SignupRequest;
import dev.sushaanth.bookly.security.model.EmployeeInvitation;
import dev.sushaanth.bookly.security.model.LibraryUser;
import dev.sushaanth.bookly.security.model.Role;
import dev.sushaanth.bookly.security.repository.EmployeeInvitationRepository;
import dev.sushaanth.bookly.security.repository.LibraryUserRepository;
import dev.sushaanth.bookly.tenant.TenantService;
import dev.sushaanth.bookly.tenant.dto.TenantCreateRequest;
import dev.sushaanth.bookly.tenant.dto.TenantResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final LibraryUserRepository userRepository;
    private final EmployeeInvitationRepository employeeInvitationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;
    private final TenantService tenantService;

    public AuthService(AuthenticationManager authenticationManager,
                       LibraryUserRepository userRepository,
                       EmployeeInvitationRepository employeeInvitationRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenUtil jwtTokenUtil,
                       TenantService tenantService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.employeeInvitationRepository = employeeInvitationRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenUtil = jwtTokenUtil;
        this.tenantService = tenantService;
    }

    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.username(),
                        loginRequest.password()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // Get the user to retrieve tenant information
        LibraryUser user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String jwt = jwtTokenUtil.generateToken(
                user.getUsername(),
                user.getTenantId(),
                user.getRole()
        );

        return new JwtResponse(jwt, user.getUsername(), user.getRole().name());
    }

    @Transactional
    public void registerLibrary(SignupRequest signupRequest) {
        // Create a new tenant for the library
        TenantCreateRequest tenantRequest = new TenantCreateRequest(
                signupRequest.libraryName(),
                "Library tenant for " + signupRequest.libraryName()
        );

        TenantResponse tenantResponse = tenantService.createTenant(tenantRequest);

        // Create the library admin user
        LibraryUser user = new LibraryUser();
        user.setUsername(signupRequest.username());
        user.setEmail(signupRequest.email());
        user.setFirstName(signupRequest.firstName());
        user.setLastName(signupRequest.lastName());
        user.setPassword(passwordEncoder.encode(signupRequest.password()));
        user.setRole(Role.ROLE_LIBRARY_ADMIN);

        // Directly set tenant ID from the response
        user.setTenantId(tenantResponse.id());

        userRepository.save(user);
    }

    @Transactional
    public void registerEmployee(SignupRequest signupRequest) {
        // Validate that the email is in the pending employees list
        // This should check against a repository of invited employees
        EmployeeInvitation invitation = employeeInvitationRepository
                .findByEmailAndUsedFalse(signupRequest.email())
                .orElseThrow(() -> new RuntimeException("No valid invitation found for this email"));

        LibraryUser user = new LibraryUser();
        user.setUsername(signupRequest.username());
        user.setEmail(signupRequest.email());
        user.setFirstName(signupRequest.firstName());
        user.setLastName(signupRequest.lastName());
        user.setPassword(passwordEncoder.encode(signupRequest.password()));
        user.setRole(Role.ROLE_EMPLOYEE);

        // Set tenant ID from the invitation
        user.setTenantId(invitation.getTenantId());

        // Mark invitation as used
        invitation.setUsed(true);
        employeeInvitationRepository.save(invitation);

        userRepository.save(user);
    }
}